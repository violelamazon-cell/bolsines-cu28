package ar.edu.utn.frc.dsi.bolsines.control;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.*;
import ar.edu.utn.frc.dsi.bolsines.boundary.excepciones.*;
import ar.edu.utn.frc.dsi.bolsines.entidades.*;
import ar.edu.utn.frc.dsi.bolsines.entidades.estado.RecibidaYAceptada;
import ar.edu.utn.frc.dsi.bolsines.repositorios.BolsinRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para GestorRecepcionBolsin (Fase 3 - Parte B).
 *
 * Ejecuta sobre el perfil 'test' (H2 en memoria, create-drop) con datos sembrados por DataLoader.
 * Verifica la lógica del Gestor, el Patrón Experto y el cambio de estado con el Patrón State.
 */
@SpringBootTest
@ActiveProfiles("test")
class GestorRecepcionBolsinTests {

    @Autowired
    private GestorRecepcionBolsin gestor;

    @Autowired
    private BolsinRepository bolsinRepository;

    @Autowired
    private ar.edu.utn.frc.dsi.bolsines.repositorios.EstadoRemitoRepository estadoRemitoRepository;

    // IDs de la semilla DataLoader:
    // 1 -> Carlos Gómez (CM Córdoba)
    // 4 -> Laura Torres (CM Villa María, sin bolsines enviados)
    private static final Long EMP_CORDOBA = 1L;
    private static final Long EMP_VILLA_MARIA = 4L;

    @Test
    @Transactional
    @DisplayName("Flujo principal completo: Gestor actualiza bolsín, remitos y documentaciones con historial coherente")
    void flujoPrincipalCompleto_actualizaEstadosYRegistraHistorial() {
        int nroBolsin = 1001;

        // 1. Paso 1-4: Iniciar recepción
        IniciarRecepcionResponse inicio = gestor.registrarRecepcionBolsin(EMP_CORDOBA);
        assertNotNull(inicio);
        assertEquals("CM Córdoba", inicio.cmUsuario());
        assertTrue(inicio.bolsines().stream().anyMatch(b -> b.numeroBolsin() == nroBolsin));

        // 2. Paso 5-7: Seleccionar bolsín
        SeleccionarBolsinResponse seleccion = gestor.tomarSeleccionBolsin(EMP_CORDOBA, nroBolsin);
        assertEquals(nroBolsin, seleccion.numeroBolsin());
        assertEquals(2, seleccion.remitos().size());
        assertEquals(4, seleccion.opcionesRecepcion().size());
        assertTrue(seleccion.opcionesRecepcion().get(0).habilitada());
        assertFalse(seleccion.opcionesRecepcion().get(1).habilitada());

        // 3. Paso 8-9: Seleccionar opción 1
        SeleccionarRecepcionResponse opcionRes = gestor.tomarSeleccionRecepcion(EMP_CORDOBA, nroBolsin, 1);
        assertEquals(1, opcionRes.opcion());
        assertTrue(opcionRes.requiereConfirmacion());

        // 4. Paso 10-12: Confirmar recepción
        ConfirmarRecepcionResponse confirmacion = gestor.tomarConfirmacion(EMP_CORDOBA, nroBolsin, 1, true);
        assertTrue(confirmacion.confirmado());
        assertEquals(nroBolsin, confirmacion.numeroBolsin());
        assertEquals("Recibido en CM destino", confirmacion.nuevoEstadoBolsin());
        assertEquals("Recibido y Aceptado", confirmacion.nuevoEstadoRemitos());
        assertEquals("Recibida y aceptada", confirmacion.nuevoEstadoDocumentaciones());
        assertEquals(2, confirmacion.remitosActualizados());
        assertEquals(3, confirmacion.documentacionesActualizadas());

        // Aserción: una notificación por CM de origen distinta
        assertEquals(1, confirmacion.notificaciones().size(), "Debe emitirse exactamente una notificación para CM Mendoza");
        NotificacionDto notif = confirmacion.notificaciones().get(0);
        assertEquals("CM Mendoza", notif.cmOrigen());
        assertEquals("mendoza@bolsines.gov.ar", notif.email());
        assertTrue(notif.asunto().contains(String.valueOf(nroBolsin)));

        // 5. Verificación directa en base de datos
        Bolsin bolsinBd = bolsinRepository.findByNumeroBolsin(nroBolsin).orElseThrow();
        assertFalse(bolsinBd.sosEnviado(), "El bolsín 1001 ya no debe estar en estado Enviado");

        CambioEstadoBolsin cambioBolsinActual = bolsinBd.buscarEstadoActual();
        assertNotNull(cambioBolsinActual);
        assertEquals("Recibido en CM destino", cambioBolsinActual.getEstado().getNombre());
        assertNull(cambioBolsinActual.getFechaHoraFin());
        assertNotNull(cambioBolsinActual.getFechaHoraInicio());
        assertEquals("Carlos", cambioBolsinActual.getResponsableCE().getNombre());

        for (Remito r : bolsinBd.getRemito()) {
            assertEquals("Recibido y Aceptado", r.getEstado().getNombre(), "Cada remito debe pasar a Recibido y Aceptado");
            for (DetalleRemito d : r.getDetalleRemito()) {
                Documentacion doc = d.buscarDocumentacionRemito();
                assertTrue(doc.getEstado() instanceof RecibidaYAceptada, "La documentación debe estar en estado RecibidaYAceptada (State)");
                assertEquals("Recibida y aceptada", doc.getEstado().getNombre());

                // Historial de la documentación: cambio actual
                CambioEstadoDocumentacion cambioDocActual = doc.getCambioEstado().stream()
                        .filter(CambioEstadoDocumentacion::esActual)
                        .findFirst()
                        .orElseThrow();
                assertEquals("Recibida y aceptada", cambioDocActual.getEstado().getNombre());
                assertNull(cambioDocActual.getFechaHoraFin());
                assertNotNull(cambioDocActual.getFechaHoraInicio());
                assertNotNull(cambioDocActual.getResponsableCE());
                assertEquals("Carlos", cambioDocActual.getResponsableCE().getNombre());

                // Historial de la documentación: cambio anterior cerrado con fechaHoraFin
                List<CambioEstadoDocumentacion> cambiosCerrados = doc.getCambioEstado().stream()
                        .filter(c -> !c.esActual())
                        .toList();
                assertFalse(cambiosCerrados.isEmpty(), "Debe existir al menos un cambio cerrado");
                CambioEstadoDocumentacion cambioAnterior = cambiosCerrados.get(cambiosCerrados.size() - 1);
                assertEquals("En bolsín enviado", cambioAnterior.getEstado().getNombre());
                assertNotNull(cambioAnterior.getFechaHoraFin(), "El cambio anterior debe tener fechaHoraFin asignada");
                assertEquals(cambioDocActual.getFechaHoraInicio(), cambioAnterior.getFechaHoraFin(),
                        "La fechaHoraFin del cambio anterior debe coincidir con la fechaHoraInicio del cambio actual");
            }
        }
    }

    @Test
    @DisplayName("Filtro A1: Precinto inexistente lanza RecursoNoEncontradoException")
    void filtroA1_precintoInexistente_lanzaExcepcion() {
        RecursoNoEncontradoException ex = assertThrows(
                RecursoNoEncontradoException.class,
                () -> gestor.filtrarBolsines(EMP_CORDOBA, 99999, null)
        );
        assertTrue(ex.getMessage().contains("precinto"), "El mensaje debe aludir al número de precinto");
    }

    @Test
    @DisplayName("Filtro A2: CM de origen inexistente lanza RecursoNoEncontradoException")
    void filtroA2_cmOrigenInexistente_lanzaExcepcion() {
        RecursoNoEncontradoException ex = assertThrows(
                RecursoNoEncontradoException.class,
                () -> gestor.filtrarBolsines(EMP_CORDOBA, null, "CM Inexistente Tierra del Fuego")
        );
        assertTrue(ex.getMessage().contains("comisión médica"), "El mensaje debe aludir a la comisión médica de origen");
    }

    @Test
    @DisplayName("Estado vacío: empleado de CM Villa María sin bolsines enviados lanza RecursoNoEncontradoException")
    void estadoVacio_empleadoVillaMaria_lanzaExcepcion() {
        RecursoNoEncontradoException ex = assertThrows(
                RecursoNoEncontradoException.class,
                () -> gestor.registrarRecepcionBolsin(EMP_VILLA_MARIA)
        );
        assertTrue(ex.getMessage().contains("No hay bolsines enviados"), "El mensaje debe indicar ausencia de bolsines pendientes");
    }

    @Test
    @DisplayName("Doble confirmación: intentar confirmar un bolsín ya recibido lanza ConflictoEstadoException")
    void dobleConfirmacion_lanzaConflicto() {
        int nroBolsin = 1002;

        // Primera confirmación exitosa
        ConfirmarRecepcionResponse primera = gestor.tomarConfirmacion(EMP_CORDOBA, nroBolsin, 1, true);
        assertTrue(primera.confirmado());

        // Segunda confirmación sobre el mismo bolsín
        ConflictoEstadoException ex = assertThrows(
                ConflictoEstadoException.class,
                () -> gestor.tomarConfirmacion(EMP_CORDOBA, nroBolsin, 1, true)
        );
        assertTrue(ex.getMessage().contains("1002"), "El mensaje debe referenciar el número de bolsín en conflicto");
    }

    @Test
    @DisplayName("Opciones 2, 3 y 4: lanzan FuncionalidadNoDisponibleException sin modificar la base de datos")
    void opcionesNoDisponibles_lanzanExcepcion() {
        int nroBolsin = 1003;

        assertThrows(FuncionalidadNoDisponibleException.class,
                () -> gestor.tomarSeleccionRecepcion(EMP_CORDOBA, nroBolsin, 2));

        assertThrows(FuncionalidadNoDisponibleException.class,
                () -> gestor.tomarSeleccionRecepcion(EMP_CORDOBA, nroBolsin, 3));

        assertThrows(FuncionalidadNoDisponibleException.class,
                () -> gestor.tomarSeleccionRecepcion(EMP_CORDOBA, nroBolsin, 4));

        assertThrows(FuncionalidadNoDisponibleException.class,
                () -> gestor.tomarConfirmacion(EMP_CORDOBA, nroBolsin, 2, true));
    }

    @Test
    @DisplayName("Rollback: Si ocurre un fallo a mitad de tomarConfirmacion, no queda ningún estado modificado en BD")
    void rollback_errorAMitadDeConfirmacion_noModificaEstados() {
        int nroBolsin = 1003;

        // Modificamos temporalmente el nombre del estado en catálogo para provocar un error una vez iniciado el cambio en memoria
        EstadoRemito estadoRemitoOriginal = estadoRemitoRepository.findByNombre("Recibido y Aceptado").orElseThrow();
        estadoRemitoOriginal.setNombre("Recibido y Aceptado TEMPORAL");
        estadoRemitoRepository.save(estadoRemitoOriginal);

        try {
            // Se ejecuta la llamada: dentro de tomarConfirmacion se llama a recibirBolsin()
            // pero falla inmediatamente al buscar el estado de remito en el catálogo -> rollback total
            assertThrows(IllegalStateException.class, () ->
                    gestor.tomarConfirmacion(EMP_CORDOBA, nroBolsin, 1, true)
            );
        } finally {
            // Restauramos el nombre original del catálogo
            estadoRemitoOriginal.setNombre("Recibido y Aceptado");
            estadoRemitoRepository.save(estadoRemitoOriginal);
        }

        // Verificamos que la BD hizo rollback completo: el bolsín 1003 sigue en estado Enviado
        Bolsin b1003 = bolsinRepository.findByNumeroBolsinWithCambios(nroBolsin).orElseThrow();
        assertTrue(b1003.sosEnviado(), "Rollback: Bolsín 1003 debe seguir en estado Enviado");
        assertNotEquals("Recibido en CM destino", b1003.buscarEstadoActual().getEstado().getNombre(),
                "Rollback: El bolsín 1003 no debe haber quedado en Recibido en CM destino");
    }

    @Test
    @Transactional
    @DisplayName("Flujo A6: No confirmar (confirmado=false) devuelve confirmado=false sin modificar datos")
    void flujoA6_noConfirmar_noModificaBD() {
        int nroBolsin = 1003;

        ConfirmarRecepcionResponse respuesta = gestor.tomarConfirmacion(EMP_CORDOBA, nroBolsin, 1, false);

        assertFalse(respuesta.confirmado());
        assertTrue(respuesta.mensaje().contains("No se registró la recepción"));

        // El bolsín 1003 debe seguir en estado Enviado
        Bolsin b1003 = bolsinRepository.findByNumeroBolsin(nroBolsin).orElseThrow();
        assertTrue(b1003.sosEnviado(), "El bolsín 1003 debe conservar su estado Enviado");
    }
}
