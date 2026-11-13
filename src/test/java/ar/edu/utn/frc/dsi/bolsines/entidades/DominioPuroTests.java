package ar.edu.utn.frc.dsi.bolsines.entidades;

import ar.edu.utn.frc.dsi.bolsines.entidades.estado.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios de lógica de dominio pura (POO).
 *
 * No dependen de Spring Boot, ni de Hibernate, ni de base de datos alguna.
 * Instancian y prueban las entidades y el Patrón State en memoria pura.
 */
class DominioPuroTests {

    @Test
    @DisplayName("Patrón State: EnBolsinEnviado.aceptar() cierra cambio anterior y pasa a Recibida y aceptada")
    void testEnBolsinEnviadoAceptar() {
        LocalDateTime ahora = LocalDateTime.now();
        ComisionMedica cmOrigen = new ComisionMedica("CM-MZA", "CM Mendoza", "San Martin 100", "m@m.com", "123");
        TipoDocumento tipo = new TipoDocumento("Expediente", "Expediente de prueba");
        Empleado respEnvio = new Empleado("Ana", "Martínez", "ana@mendoza.gov.ar", cmOrigen);
        Empleado respRecepcion = new Empleado("Carlos", "Gómez", "carlos@cordoba.gov.ar", null);

        // Documentación en estado inicial EnBolsinEnviado
        Documentacion doc = new Documentacion(3001, "Expediente López", ahora.minusDays(2), cmOrigen, tipo, new EnBolsinEnviado());

        // Cambio de estado vigente previo (sin fecha fin)
        CambioEstadoDocumentacion cambioPrevio = new CambioEstadoDocumentacion(ahora.minusDays(1), null, new EnBolsinEnviado(), respEnvio);
        doc.agregarCambioEstado(cambioPrevio);

        assertTrue(cambioPrevio.esActual(), "El cambio previo debe ser actual antes de la transición");
        assertEquals(EnBolsinEnviado.NOMBRE, doc.getEstado().getNombre());

        // EJECUCIÓN: CU 28 Paso 11c
        doc.aceptar(ahora, respRecepcion);

        // 1. El estado de la documentación debe haber cambiado a RecibidaYAceptada
        assertTrue(doc.getEstado() instanceof RecibidaYAceptada, "El nuevo estado debe ser instancia de RecibidaYAceptada");
        assertEquals(RecibidaYAceptada.NOMBRE, doc.getEstado().getNombre());

        // 2. El cambio anterior debe haber sido cerrado con fechaHoraFin = ahora
        assertFalse(cambioPrevio.esActual(), "El cambio anterior ya no debe ser actual");
        assertEquals(ahora, cambioPrevio.getFechaHoraFin(), "La fechaHoraFin del cambio previo debe ser 'ahora'");

        // 3. Se debe haber agregado un nuevo CambioEstadoDocumentacion al historial
        assertEquals(2, doc.getCambioEstado().size(), "El historial debe contener 2 cambios");
        CambioEstadoDocumentacion cambioNuevo = doc.getCambioEstado().get(1);

        assertTrue(cambioNuevo.esActual(), "El nuevo cambio debe ser el vigente (fechaHoraFin == null)");
        assertNull(cambioNuevo.getFechaHoraFin());
        assertEquals(ahora, cambioNuevo.getFechaHoraInicio());
        assertEquals(respRecepcion, cambioNuevo.getResponsableCE(), "El responsable debe ser el empleado que recibió");
        assertEquals(RecibidaYAceptada.NOMBRE, cambioNuevo.getEstado().getNombre());
    }

    @Test
    @DisplayName("Patrón State: Transición no permitida lanza IllegalStateException")
    void testTransicionNoPermitidaLanzaExcepcion() {
        LocalDateTime ahora = LocalDateTime.now();
        Empleado empleado = new Empleado("Carlos", "Gómez", "carlos@cordoba.gov.ar", null);

        // Una documentación en estado 'Registrada' NO puede ser 'aceptada' directamente en el CU 28
        Documentacion docRegistrada = new Documentacion(9001, "Doc Nueva", ahora, null, null, new Registrada());
        docRegistrada.agregarCambioEstado(new CambioEstadoDocumentacion(ahora, null, new Registrada(), empleado));

        IllegalStateException ex1 = assertThrows(IllegalStateException.class, () ->
                docRegistrada.aceptar(ahora, empleado)
        );
        assertTrue(ex1.getMessage().contains("no permitida"), "El mensaje de error debe indicar transición no permitida");
        assertTrue(ex1.getMessage().contains("Registrada"));

        // Tampoco se puede rechazar o redirigir desde Registrada
        assertThrows(IllegalStateException.class, () -> docRegistrada.rechazar(ahora, empleado));
        assertThrows(IllegalStateException.class, () -> docRegistrada.redirigir(ahora, empleado));

        // Una documentación ya 'Recibida y aceptada' tampoco permite volver a ejecutarse
        Documentacion docAceptada = new Documentacion(9002, "Doc Final", ahora, null, null, new RecibidaYAceptada());
        assertThrows(IllegalStateException.class, () -> docAceptada.aceptar(ahora, empleado));
    }

    @Test
    @DisplayName("Patrón Experto: Bolsin.sosEnviado() responde según su cambio de estado actual")
    void testBolsinSosEnviado() {
        LocalDateTime ahora = LocalDateTime.now();
        Empleado emp = new Empleado("Ana", "Martínez", "ana@mza.com", null);
        EstadoBolsin ebCreado = new EstadoBolsin(EstadoBolsin.CREADO, "Creado");
        EstadoBolsin ebEnviado = new EstadoBolsin(EstadoBolsin.ENVIADO, "Enviado");

        Bolsin bolsin = new Bolsin(1001, 50001, ahora.minusDays(2), 1000.0, null, null);

        // Sin cambios de estado -> false
        assertFalse(bolsin.sosEnviado());

        // Con cambio 'Creado' vigente -> false
        CambioEstadoBolsin cCreado = new CambioEstadoBolsin(ahora.minusDays(2), null, ebCreado, emp);
        bolsin.agregarCambioEstado(cCreado);
        assertFalse(bolsin.sosEnviado());

        // Se cierra 'Creado' y se agrega 'Enviado' vigente -> true
        cCreado.setFechaHoraFin(ahora.minusDays(1));
        CambioEstadoBolsin cEnviado = new CambioEstadoBolsin(ahora.minusDays(1), null, ebEnviado, emp);
        bolsin.agregarCambioEstado(cEnviado);
        assertTrue(bolsin.sosEnviado(), "Debe responder true cuando el cambio actual tiene estado 'Enviado'");

        // Si se cierra 'Enviado' -> false
        cEnviado.setFechaHoraFin(ahora);
        assertFalse(bolsin.sosEnviado(), "Debe responder false si el cambio 'Enviado' ya fue cerrado");
    }

    @Test
    @DisplayName("Patrón Experto: Bolsin.recibir() cierra cambio vigente y agrega 'Recibido en CM destino'")
    void testBolsinRecibir() {
        LocalDateTime ahora = LocalDateTime.now();
        Empleado respEnvio = new Empleado("Ana", "Martínez", "ana@mza.com", null);
        Empleado respRecepcion = new Empleado("Carlos", "Gómez", "carlos@cba.com", null);

        EstadoBolsin ebEnviado = new EstadoBolsin(EstadoBolsin.ENVIADO, "Enviado");
        EstadoBolsin ebRecibido = new EstadoBolsin(EstadoBolsin.RECIBIDO_EN_CM_DESTINO, "Recibido en CM destino");

        Bolsin bolsin = new Bolsin(1001, 50001, ahora.minusDays(1), 1200.0, null, null);
        CambioEstadoBolsin cEnviado = new CambioEstadoBolsin(ahora.minusDays(1), null, ebEnviado, respEnvio);
        bolsin.agregarCambioEstado(cEnviado);

        assertTrue(bolsin.sosEnviado());

        // EJECUCIÓN: CU 28 Paso 11a
        bolsin.recibir(ahora, respRecepcion, ebRecibido);

        // 1. Cambio anterior cerrado
        assertFalse(cEnviado.esActual());
        assertEquals(ahora, cEnviado.getFechaHoraFin());

        // 2. Nuevo cambio vigente agregado
        assertEquals(2, bolsin.getCambioEstado().size());
        CambioEstadoBolsin cNuevo = bolsin.buscarEstadoActual();
        assertNotNull(cNuevo);
        assertTrue(cNuevo.esActual());
        assertEquals(ebRecibido, cNuevo.getEstado());
        assertEquals(respRecepcion, cNuevo.getResponsableCE());
        assertEquals(ahora, cNuevo.getFechaHoraInicio());

        // 3. Ya no es enviado
        assertFalse(bolsin.sosEnviado());
    }

    @Test
    @DisplayName("Integración pura de dominio: recibirYAceptarRemitos propaga la actualización al State de Documentacion")
    void testRecibirYAceptarRemitosPropagacion() {
        LocalDateTime ahora = LocalDateTime.now();
        Empleado resp = new Empleado("Carlos", "Gómez", "carlos@cba.com", null);

        EstadoRemito erEnviado = new EstadoRemito(EstadoRemito.EN_BOLSIN_ENVIADO, "Enviado");
        EstadoRemito erRecibido = new EstadoRemito(EstadoRemito.RECIBIDO_Y_ACEPTADO, "Recibido y Aceptado");

        Bolsin bolsin = new Bolsin(1001, 50001, ahora, 1000.0, null, null);
        Remito remito = new Remito(2001, ahora, null, null, erEnviado);

        Documentacion doc1 = new Documentacion(1, "Doc 1", ahora, null, null, new EnBolsinEnviado());
        doc1.agregarCambioEstado(new CambioEstadoDocumentacion(ahora.minusHours(5), null, new EnBolsinEnviado(), resp));

        Documentacion doc2 = new Documentacion(2, "Doc 2", ahora, null, null, new EnBolsinEnviado());
        doc2.agregarCambioEstado(new CambioEstadoDocumentacion(ahora.minusHours(5), null, new EnBolsinEnviado(), resp));

        remito.agregarDetalle(new DetalleRemito(doc1, null));
        remito.agregarDetalle(new DetalleRemito(doc2, null));
        bolsin.agregarRemito(remito);

        // EJECUCIÓN: CU 28 Paso 11b + 11c
        bolsin.recibirYAceptarRemitos(erRecibido, ahora, resp);

        // Verificaciones
        assertEquals(erRecibido, remito.getEstado(), "El remito debe quedar en Recibido y Aceptado");
        assertEquals(RecibidaYAceptada.NOMBRE, doc1.getEstado().getNombre(), "Doc 1 debe haber pasado a Recibida y aceptada");
        assertEquals(RecibidaYAceptada.NOMBRE, doc2.getEstado().getNombre(), "Doc 2 debe haber pasado a Recibida y aceptada");
        assertTrue(doc1.getEstado() instanceof RecibidaYAceptada);
        assertTrue(doc2.getEstado() instanceof RecibidaYAceptada);
    }

    @Test
    @DisplayName("EstadoFactory: Resuelve nombres válidos y lanza IllegalArgumentException en no reconocidos")
    void testEstadoFactory() {
        assertInstanceOf(EnBolsinEnviado.class, EstadoFactory.porNombre("En bolsín enviado"));
        assertInstanceOf(EnBolsinEnviado.class, EstadoFactory.porNombre("en bolsin enviado"));
        assertInstanceOf(EnBolsinEnviado.class, EstadoFactory.porNombre("EnBolsinEnviado"));
        assertInstanceOf(RecibidaYAceptada.class, EstadoFactory.porNombre("Recibida y aceptada"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                EstadoFactory.porNombre("EstadoInventado")
        );
        assertTrue(ex.getMessage().contains("no reconocido"));

        assertThrows(IllegalArgumentException.class, () -> EstadoFactory.porNombre(null));
        assertThrows(IllegalArgumentException.class, () -> EstadoFactory.porNombre("  "));
    }

    @Test
    @DisplayName("Estado: equals y hashCode consistentes por nombre")
    void testEstadoEqualsHashCode() {
        Estado e1 = new EnBolsinEnviado();
        Estado e2 = new EnBolsinEnviado();
        Estado e3 = new RecibidaYAceptada();

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1, e3);
    }
}

