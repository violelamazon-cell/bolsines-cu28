package ar.edu.utn.frc.dsi.bolsines;

import ar.edu.utn.frc.dsi.bolsines.entidades.*;
import ar.edu.utn.frc.dsi.bolsines.repositorios.BolsinRepository;
import ar.edu.utn.frc.dsi.bolsines.repositorios.ComisionMedicaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración REST con MockMvc para el CU 28 (Fase 3 - Parte C).
 *
 * Ejecuta sobre el perfil 'test' (H2 en memoria, create-drop).
 * Verifica los endpoints del boundary PantallaRegistrarRecepcionBolsin,
 * el controlador SesionController y el manejo uniforme de errores de ManejadorExcepciones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecepcionBolsinIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BolsinRepository bolsinRepository;

    @Autowired
    private ComisionMedicaRepository comisionMedicaRepository;

    private static final String EMP_CORDOBA = "1";
    private static final String EMP_VILLA_MARIA = "4";

    // =========================================================================
    // Sesión simulada
    // =========================================================================

    @Test
    @DisplayName("GET /api/sesion -> retorna datos del empleado activo")
    void getSesion_retornaDatosEmpleado() throws Exception {
        mockMvc.perform(get("/api/sesion").param("empleadoId", EMP_CORDOBA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empleadoId").value(1))
                .andExpect(jsonPath("$.nombreCompleto").isNotEmpty())
                .andExpect(jsonPath("$.codigoCm").value("CMJ-CBA"));
    }

    @Test
    @DisplayName("GET /api/sesion/empleados -> lista todos los empleados")
    void listarEmpleados_retornaLista() throws Exception {
        mockMvc.perform(get("/api/sesion/empleados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    @DisplayName("POST /api/sesion/iniciar -> cambia sesión y retorna datos")
    void iniciarSesion_cambiaUsuario() throws Exception {
        String body = """
                {"empleadoId": 4}
                """;
        mockMvc.perform(post("/api/sesion/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empleadoId").value(4))
                .andExpect(jsonPath("$.codigoCm").value("CMJ-VM"));
    }

    // =========================================================================
    // Flujo principal completo
    // =========================================================================

    @Test
    @DisplayName("POST /api/recepcion-bolsin/iniciar -> retorna 3 bolsines enviados para CM Córdoba")
    void iniciar_retornaBolsinesEnviadosParaCordoba() throws Exception {
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cmUsuario").value("CM Córdoba"))
                .andExpect(jsonPath("$.bolsines", hasSize(3)))
                .andExpect(jsonPath("$.bolsines[*].numeroBolsin", containsInAnyOrder(1001, 1002, 1003)));
    }

    @Test
    @DisplayName("GET /api/recepcion-bolsin/bolsines -> sin filtros retorna los 3 bolsines enviados")
    void bolsines_sinFiltros_retornaTodos() throws Exception {
        mockMvc.perform(get("/api/recepcion-bolsin/bolsines")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("POST /api/recepcion-bolsin/seleccionar-bolsin -> retorna detalle del bolsín 1001")
    void seleccionarBolsin_retornaDetalle() throws Exception {
        String body = """
                {"numeroBolsin": 1001}
                """;

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-bolsin")
                        .header("X-Empleado-Id", EMP_CORDOBA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroBolsin").value(1001))
                .andExpect(jsonPath("$.remitos", hasSize(2)))
                .andExpect(jsonPath("$.opcionesRecepcion", hasSize(4)))
                .andExpect(jsonPath("$.opcionesRecepcion[0].habilitada").value(true))
                .andExpect(jsonPath("$.opcionesRecepcion[1].habilitada").value(false));
    }

    @Test
    @DisplayName("POST /api/recepcion-bolsin/seleccionar-recepcion opcion=1 -> requiereConfirmacion=true")
    void seleccionarRecepcion_opcion1_requiereConfirmacion() throws Exception {
        String body = """
                {"numeroBolsin": 1001, "opcion": 1}
                """;

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-recepcion")
                        .header("X-Empleado-Id", EMP_CORDOBA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opcion").value(1))
                .andExpect(jsonPath("$.requiereConfirmacion").value(true));
    }

    @Test
    @Transactional
    @DisplayName("POST /api/recepcion-bolsin/confirmar -> actualiza estados en BD y genera notificaciones")
    void confirmarRecepcion_flujoPrincipal_actualizaEstados() throws Exception {
        String body = """
                {"numeroBolsin": 1002, "opcion": 1, "confirmado": true}
                """;

        mockMvc.perform(post("/api/recepcion-bolsin/confirmar")
                        .header("X-Empleado-Id", EMP_CORDOBA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmado").value(true))
                .andExpect(jsonPath("$.numeroBolsin").value(1002))
                .andExpect(jsonPath("$.nuevoEstadoBolsin").value("Recibido en CM destino"))
                .andExpect(jsonPath("$.remitosActualizados").value(greaterThan(0)))
                .andExpect(jsonPath("$.documentacionesActualizadas").value(greaterThan(0)))
                .andExpect(jsonPath("$.notificaciones", hasSize(greaterThanOrEqualTo(1))));

        Bolsin b1002 = bolsinRepository.findByNumeroBolsin(1002).orElseThrow();
        b1002.getCambioEstado().size();
        assertFalse(b1002.sosEnviado());
        assertEquals("Recibido en CM destino", b1002.buscarEstadoActual().getEstado().getNombre());
    }

    // =========================================================================
    // Flujo A6: No confirmar
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("POST /api/recepcion-bolsin/confirmar con confirmado=false (A6) -> no modifica datos")
    void confirmarRecepcion_noConfirmado_noModificaBD() throws Exception {
        String body = """
                {"numeroBolsin": 1003, "opcion": 1, "confirmado": false}
                """;

        mockMvc.perform(post("/api/recepcion-bolsin/confirmar")
                        .header("X-Empleado-Id", EMP_CORDOBA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmado").value(false))
                .andExpect(jsonPath("$.mensaje").value(containsString("No se registró")));

        Bolsin b1003 = bolsinRepository.findByNumeroBolsin(1003).orElseThrow();
        b1003.getCambioEstado().size();
        assertTrue(b1003.sosEnviado());
    }

    // =========================================================================
    // Doble confirmación -> 409 Conflict
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("POST /api/recepcion-bolsin/confirmar dos veces el mismo bolsín -> segundo intento devuelve 409")
    void confirmarRecepcion_dobleConfirmacion_devuelve409() throws Exception {
        String body = """
                {"numeroBolsin": 1001, "opcion": 1, "confirmado": true}
                """;

        // Primer envío -> 200
        mockMvc.perform(post("/api/recepcion-bolsin/confirmar")
                        .header("X-Empleado-Id", EMP_CORDOBA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmado").value(true));

        // Segundo envío -> 409
        mockMvc.perform(post("/api/recepcion-bolsin/confirmar")
                        .header("X-Empleado-Id", EMP_CORDOBA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("1001")));
    }

    // =========================================================================
    // Filtros A1 y A2 -> 404 Not Found
    // =========================================================================

    @Test
    @DisplayName("GET /api/recepcion-bolsin/bolsines?numeroPrecinto=99999 (A1) -> 404 con mensaje")
    void filtrarBolsines_precintoInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/recepcion-bolsin/bolsines")
                        .header("X-Empleado-Id", EMP_CORDOBA)
                        .param("numeroPrecinto", "99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value(containsString("precinto")));
    }

    @Test
    @DisplayName("GET /api/recepcion-bolsin/bolsines?cmOrigen=CM+Inexistente (A2) -> 404 con mensaje")
    void filtrarBolsines_cmOrigenInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/recepcion-bolsin/bolsines")
                        .header("X-Empleado-Id", EMP_CORDOBA)
                        .param("cmOrigen", "CM Inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value(containsString("comisión médica")));
    }

    // =========================================================================
    // Estado vacío -> 404 Not Found
    // =========================================================================

    @Test
    @DisplayName("POST /api/recepcion-bolsin/iniciar con empleado de Villa María -> 404")
    void iniciar_empleadoVillaMaria_sinBolsines_devuelve404() throws Exception {
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_VILLA_MARIA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value(containsString("bolsines enviados pendientes")));
    }

    // =========================================================================
    // Opción no disponible -> 501 Not Implemented
    // =========================================================================

    @Test
    @DisplayName("POST /api/recepcion-bolsin/seleccionar-recepcion opcion=2 -> 501")
    void seleccionarRecepcion_opcion2_devuelve501() throws Exception {
        String body = """
                {"numeroBolsin": 1001, "opcion": 2}
                """;

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-recepcion")
                        .header("X-Empleado-Id", EMP_CORDOBA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.mensaje").value(containsString("Próximamente")));
    }

    // =========================================================================
    // Cancelar -> 204 No Content
    // =========================================================================

    @Test
    @DisplayName("POST /api/recepcion-bolsin/cancelar -> 204")
    void cancelar_devuelve204() throws Exception {
        mockMvc.perform(post("/api/recepcion-bolsin/cancelar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isNoContent());
    }
}

