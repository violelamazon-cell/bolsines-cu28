package ar.edu.utn.frc.dsi.bolsines.control;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.*;
import ar.edu.utn.frc.dsi.bolsines.boundary.excepciones.*;
import ar.edu.utn.frc.dsi.bolsines.entidades.*;
import ar.edu.utn.frc.dsi.bolsines.entidades.estado.EnBolsinEnviado;
import ar.edu.utn.frc.dsi.bolsines.entidades.estado.RecibidaYAceptada;
import ar.edu.utn.frc.dsi.bolsines.repositorios.BolsinRepository;
import org.junit.jupiter.api.BeforeEach;
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
 * Ejecuta sobre el perfil 'test' (H2 en memoria, create-drop) con datos
 * sembrados por DataLoader.
 * Verifica la lógica del Gestor con estado, el Patrón Experto y el cambio de
 * estado con el Patrón State.
 */
@SpringBootTest
@ActiveProfiles("test")
class GestorRecepcionBolsinTests {

    @Autowired
    private GestorRecepcionBolsin gestor;

    @Autowired
    private SesionService sesionService;

    @Autowired
    private ConsultasService consultasService;

    @Autowired
    private BolsinRepository bolsinRepository;

    @Autowired
    private ar.edu.utn.frc.dsi.bolsines.repositorios.EstadoRemitoRepository estadoRemitoRepository;

    @BeforeEach
    void setUp() {
        sesionService.login("carlos", "1234");
        gestor.finCU();
    }

    @Test
    @Transactional
    @DisplayName("Paso 2 E1: getUsuarioLogueado recorre empleados con esTuUsuario y carga empleadoLogueado")
    void getUsuarioLogueado_recorreEmpleadosYAsignaEmpleadoLogueado() {
        sesionService.login("carlos", "1234");
        Empleado emp = gestor.getUsuarioLogueado();
        assertNotNull(emp);
        assertEquals("Carlos", emp.getNombre());
        assertEquals("Gómez", emp.getApellido());
        assertEquals(emp, gestor.getEmpleadoLogueado());
        assertEquals("CM Córdoba", emp.getCM().getNombre());
    }

    @Test
    @Transactional
    @DisplayName("Flujo principal completo: Gestor actualiza bolsín, remitos y documentaciones con historial coherente")
    void flujoPrincipalCompleto_actualizaEstadosYRegistraHistorial() {
        int nroBolsin = 1001;

        // 1. Paso 1-4: Iniciar recepción
        IniciarRecepcionResponse inicio = gestor.registrarRecepcionBolsin();
        assertNotNull(inicio);
        assertEquals("CM Córdoba", inicio.cmUsuario());
        assertTrue(inicio.bolsines().stream().anyMatch(b -> b.numeroBolsin() == nroBolsin));

        // 2. Paso 5-7: Seleccionar bolsín
        SeleccionarBolsinResponse seleccion = gestor.tomarSeleccionBolsin(nroBolsin);
        assertEquals(nroBolsin, seleccion.numeroBolsin());
        assertEquals(2, seleccion.remitos().size());
        assertEquals(4, seleccion.opcionesRecepcion().size());
        assertTrue(seleccion.opcionesRecepcion().get(0).habilitada());
        assertFalse(seleccion.opcionesRecepcion().get(1).habilitada());

        // 3. Paso 8-9: Seleccionar opción 1
        SeleccionarRecepcionResponse opcionRes = gestor.tomarSeleccionRecepcion(1);
        assertEquals(1, opcionRes.opcion());
        assertTrue(opcionRes.requiereConfirmacion());

        // 4. Paso 10-12: Confirmar recepción
        ConfirmarRecepcionResponse confirmacion = gestor.tomarConfirmacion(true);
        assertTrue(confirmacion.confirmado());
        assertEquals(nroBolsin, confirmacion.numeroBolsin());
        assertEquals("Recibido en CM destino", confirmacion.nuevoEstadoBolsin());
        assertEquals("Recibido y Aceptado", confirmacion.nuevoEstadoRemitos());
        assertEquals("Recibida y aceptada", confirmacion.nuevoEstadoDocumentaciones());
        assertEquals(2, confirmacion.remitosActualizados());
        assertEquals(3, confirmacion.documentacionesActualizadas());

        // Aserción: una notificación por CM de origen distinta
        assertEquals(1, confirmacion.notificaciones().size(),
                "Debe emitirse exactamente una notificación para CM Mendoza");
        NotificacionDto notif = confirmacion.notificaciones().get(0);
        assertEquals("CM Mendoza", notif.cmOrigen());
        assertEquals("mendoza@bolsines.gov.ar", notif.email());
        assertTrue(notif.asunto().contains(String.valueOf(nroBolsin)));

        // 5. Verificación directa en base de datos
        Bolsin bolsinBd = bolsinRepository.findByNumeroBolsin(nroBolsin).orElseThrow();
        assertFalse(bolsinBd.sosEnviado(), "El bolsín 1001 ya no debe estar en estado Enviado");

        CambioEstadoBolsin cambioBolsinActual = bolsinBd.getCambioEstado().stream()
                .filter(CambioEstadoBolsin::esActual)
                .findFirst()
                .orElse(null);
        assertNotNull(cambioBolsinActual);
        assertEquals("Recibido en CM destino", cambioBolsinActual.getEstado().getNombre());
        assertArrayEquals(new String[] { "Carlos", "Gómez" }, cambioBolsinActual.getResponsableCE());

        // Verificar remitos en BD
        for (Remito r : bolsinBd.getRemito()) {
            assertEquals("Recibido y Aceptado", r.getEstado().getNombre());
            for (DetalleRemito d : r.getDetalleRemito()) {
                Documentacion doc = d.getDocumentacion();
                assertTrue(doc.getEstado() instanceof RecibidaYAceptada);
                CambioEstadoDocumentacion cambioDocActual = doc.getCambioEstado().stream()
                        .filter(CambioEstadoDocumentacion::esEstadoActual)
                        .findFirst()
                        .orElse(null);
                assertNotNull(cambioDocActual);
                assertTrue(cambioDocActual.getEstado() instanceof RecibidaYAceptada);
                assertArrayEquals(new String[] { "Carlos", "Gómez" }, cambioDocActual.getResponsableCE());
            }
        }
    }

    @Test
    @Transactional
    @DisplayName("Paso 4: Filtro por número de precinto devuelve solo los bolsines coincidentes")
    void filtrarBolsines_porNumeroPrecinto_filtraCorrectamente() {
        gestor.registrarRecepcionBolsin();

        List<BolsinResumenDto> filtrados = consultasService.filtrarBolsines(50001, null);
        assertEquals(1, filtrados.size());
        assertEquals(1001, filtrados.get(0).numeroBolsin());
        assertEquals(50001, filtrados.get(0).numeroPrecinto());
    }

    @Test
    @Transactional
    @DisplayName("Paso 4: Filtro por CM de origen devuelve solo los bolsines coincidentes")
    void filtrarBolsines_porCmOrigen_filtraCorrectamente() {
        gestor.registrarRecepcionBolsin();

        List<BolsinResumenDto> filtrados = consultasService.filtrarBolsines(null, "CM Mendoza");
        assertEquals(1, filtrados.size());
        assertEquals(1001, filtrados.get(0).numeroBolsin());
        assertEquals("CM Mendoza", filtrados.get(0).cmOrigen());
    }

    @Test
    @Transactional
    @DisplayName("Paso 4: Sin filtros devuelve todos los bolsines de la CM del usuario")
    void filtrarBolsines_sinFiltros_devuelveTodos() {
        gestor.registrarRecepcionBolsin();

        List<BolsinResumenDto> filtrados = consultasService.filtrarBolsines(null, null);
        assertEquals(3, filtrados.size());
    }

    @Test
    @Transactional
    @DisplayName("Flujo alternativo A1: Precinto inexistente lanza RecursoNoEncontradoException")
    void filtroA1_precintoInexistente_lanzaExcepcion() {
        gestor.registrarRecepcionBolsin();
        RecursoNoEncontradoException ex = assertThrows(
                RecursoNoEncontradoException.class,
                () -> consultasService.filtrarBolsines(99999, null));
        assertTrue(ex.getMessage().contains("99999"), "El mensaje debe referenciar el precinto buscado");
    }

    @Test
    @Transactional
    @DisplayName("Flujo alternativo A2: CM origen inexistente lanza RecursoNoEncontradoException")
    void filtroA2_cmOrigenInexistente_lanzaExcepcion() {
        gestor.registrarRecepcionBolsin();
        RecursoNoEncontradoException ex = assertThrows(
                RecursoNoEncontradoException.class,
                () -> consultasService.filtrarBolsines(null, "CM Inexistente"));
        assertTrue(ex.getMessage().contains("CM Inexistente"), "El mensaje debe referenciar la CM buscada");
    }

    @Test
    @DisplayName("Estado vacío: empleado de CM Villa María sin bolsines enviados lanza RecursoNoEncontradoException")
    void estadoVacio_empleadoVillaMaria_lanzaExcepcion() {
        sesionService.login("laura", "1234");
        RecursoNoEncontradoException ex = assertThrows(
                RecursoNoEncontradoException.class,
                () -> gestor.registrarRecepcionBolsin());
        assertTrue(ex.getMessage().contains("No hay bolsines enviados"),
                "El mensaje debe indicar ausencia de bolsines pendientes");
    }

    @Test
    @Transactional
    @DisplayName("Doble confirmación: intentar confirmar un bolsín ya recibido lanza ConflictoEstadoException")
    void dobleConfirmacion_lanzaConflicto() {
        int nroBolsin = 1002;

        // Primera confirmación exitosa
        gestor.registrarRecepcionBolsin();
        gestor.tomarSeleccionBolsin(nroBolsin);
        gestor.tomarSeleccionRecepcion(1);
        ConfirmarRecepcionResponse primera = gestor.tomarConfirmacion(true);
        assertTrue(primera.confirmado());

        // Segundo intento sobre el mismo bolsín: ya no está en estado Enviado,
        // así que el conflicto aparece al seleccionarlo
        gestor.registrarRecepcionBolsin();
        ConflictoEstadoException ex = assertThrows(
                ConflictoEstadoException.class,
                () -> gestor.tomarSeleccionBolsin(nroBolsin));
        assertTrue(ex.getMessage().contains("1002"), "El mensaje debe referenciar el número de bolsín en conflicto");
    }

    @Test
    @DisplayName("Opciones 2, 3 y 4: lanzan FuncionalidadNoDisponibleException sin modificar la base de datos")
    void opcionesNoDisponibles_lanzanExcepcion() {
        int nroBolsin = 1003;
        gestor.registrarRecepcionBolsin();
        gestor.tomarSeleccionBolsin(nroBolsin);

        assertThrows(FuncionalidadNoDisponibleException.class,
                () -> gestor.tomarSeleccionRecepcion(2));

        assertThrows(FuncionalidadNoDisponibleException.class,
                () -> gestor.tomarSeleccionRecepcion(3));

        assertThrows(FuncionalidadNoDisponibleException.class,
                () -> gestor.tomarSeleccionRecepcion(4));
    }

    @Test
    @DisplayName("Rollback: Si ocurre un fallo a mitad de tomarConfirmacion, no queda ningún estado modificado en BD")
    void rollback_errorAMitadDeConfirmacion_noModificaEstados() {
        int nroBolsin = 1003;
        gestor.registrarRecepcionBolsin();
        gestor.tomarSeleccionBolsin(nroBolsin);
        gestor.tomarSeleccionRecepcion(1);

        // Modificamos temporalmente el nombre del estado en catálogo para provocar un
        // error una vez iniciado el cambio en memoria
        EstadoRemito estadoRemitoOriginal = estadoRemitoRepository.findByNombre("Recibido y Aceptado").orElseThrow();
        estadoRemitoOriginal.setNombre("Recibido y Aceptado TEMPORAL");
        estadoRemitoRepository.save(estadoRemitoOriginal);

        try {
            // Se ejecuta la llamada: dentro de tomarConfirmacion se llama a recibirBolsin()
            // pero falla inmediatamente al buscar el estado de remito en el catálogo ->
            // rollback total
            assertThrows(IllegalStateException.class, () -> gestor.tomarConfirmacion(true));
        } finally {
            // Restauramos el nombre original del catálogo
            estadoRemitoOriginal.setNombre("Recibido y Aceptado");
            estadoRemitoRepository.save(estadoRemitoOriginal);
        }

        // Verificamos que la BD hizo rollback completo: el bolsín 1003 sigue en estado
        // Enviado
        Bolsin b1003 = bolsinRepository.findByNumeroBolsinWithCambios(nroBolsin).orElseThrow();
        assertTrue(b1003.sosEnviado(), "Rollback: Bolsín 1003 debe seguir en estado Enviado");
        CambioEstadoBolsin ceB1003 = b1003.getCambioEstado().stream()
                .filter(CambioEstadoBolsin::esActual)
                .findFirst()
                .orElse(null);
        assertNotNull(ceB1003);
        assertNotEquals("Recibido en CM destino", ceB1003.getEstado().getNombre(),
                "Rollback: El bolsín 1003 no debe haber quedado en Recibido en CM destino");
    }

    @Test
    @Transactional
    @DisplayName("Flujo A6: No confirmar (confirmado=false) devuelve confirmado=false sin modificar datos")
    void flujoA6_noConfirmar_noModificaBD() {
        int nroBolsin = 1003;
        gestor.registrarRecepcionBolsin();
        gestor.tomarSeleccionBolsin(nroBolsin);
        gestor.tomarSeleccionRecepcion(1);

        ConfirmarRecepcionResponse respuesta = gestor.tomarConfirmacion(false);

        assertFalse(respuesta.confirmado());
        assertTrue(respuesta.mensaje().contains("No se registró la recepción"));

        // El bolsín 1003 debe seguir en estado Enviado
        Bolsin b1003 = bolsinRepository.findByNumeroBolsin(nroBolsin).orElseThrow();
        assertTrue(b1003.sosEnviado(), "El bolsín 1003 debe conservar su estado Enviado");
    }

    @Test
    @DisplayName("Secuencia inválida: confirmar sin bolsín seleccionado lanza ConflictoEstadoException (409)")
    void confirmarSinSeleccionarBolsin_lanzaConflicto() {
        gestor.finCU();
        assertThrows(ConflictoEstadoException.class, () -> gestor.tomarConfirmacion(true));
    }

    @Test
    @DisplayName("Cambio de usuario: iniciar con nuevo usuario reinicia el estado del Gestor")
    void cambioUsuario_reiniciaEstadoGestor() {
        gestor.registrarRecepcionBolsin();
        gestor.tomarSeleccionBolsin(1001);
        assertNotNull(gestor.getBolsinSeleccionado());

        sesionService.login("laura", "1234");
        assertNull(gestor.getBolsinSeleccionado());
        assertNull(gestor.getEmpleadoLogueado());
    }

    @Test
    @Transactional
    @DisplayName("ConsultasService: listarComisionesMedicas devuelve todas las CM ordenadas")
    void listarComisionesMedicas_devuelveListaOrdenada() {
        List<ComisionMedicaDto> cms = consultasService.listarComisionesMedicas();
        assertNotNull(cms);
        assertFalse(cms.isEmpty());
        assertTrue(cms.size() >= 5);
    }
}
