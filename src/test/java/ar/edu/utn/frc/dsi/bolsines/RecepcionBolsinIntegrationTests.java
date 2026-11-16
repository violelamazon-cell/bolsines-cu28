package ar.edu.utn.frc.dsi.bolsines;

import ar.edu.utn.frc.dsi.bolsines.control.GestorRecepcionBolsin;
import ar.edu.utn.frc.dsi.bolsines.entidades.*;
import ar.edu.utn.frc.dsi.bolsines.repositorios.BolsinRepository;
import ar.edu.utn.frc.dsi.bolsines.repositorios.ComisionMedicaRepository;
import org.junit.jupiter.api.BeforeEach;
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
 *
 * Sigue la secuencia con estado: iniciar -> seleccionar-bolsin -> seleccionar-recepcion -> confirmar.
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

    @Autowired
    private GestorRecepcionBolsin gestor;

    @Autowired
    private ar.edu.utn.frc.dsi.bolsines.control.SesionService sesionService;

    private static final String EMP_CORDOBA = "1";
    private static final String EMP_VILLA_MARIA = "4";

    @BeforeEach
    void setUp() {
        sesionService.login("carlos", "1234");
        gestor.finCU();
    }

    // =========================================================================
    // Sesión y Login/Logout (Paso 2 E1)
    // =========================================================================

    @Test
    @DisplayName("POST /api/sesion/login -> valida credenciales y crea sesión activa")
    void login_credencialesValidas_retornaSesion() throws Exception {
        String body = """
                {"usuario": "carlos", "password": "1234"}
                """;
        mockMvc.perform(post("/api/sesion/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value(containsString("Carlos")))
                .andExpect(jsonPath("$.codigoCm").value("CMJ-CBA"));
    }

    @Test
    @DisplayName("POST /api/sesion/login -> credenciales inválidas devuelve 400")
    void login_credencialesInvalidas_devuelve400() throws Exception {
        String body = """
                {"usuario": "carlos", "password": "password_erronea"}
                """;
        mockMvc.perform(post("/api/sesion/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos."));
    }

    @Test
    @DisplayName("POST /api/sesion/logout -> cierra sesión activa")
    void logout_cierraSesionActiva() throws Exception {
        mockMvc.perform(post("/api/sesion/logout"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sesion"))
                .andExpect(status().isNotFound());
    }

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
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/recepcion-bolsin/bolsines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("POST /api/recepcion-bolsin/seleccionar-bolsin -> retorna detalle del bolsín 1001")
    void seleccionarBolsin_retornaDetalle() throws Exception {
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk());

        String body = """
                {"numeroBolsin": 1001}
                """;

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-bolsin")
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
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-bolsin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroBolsin": 1001}
                                """))
                .andExpect(status().isOk());

        String body = """
                {"numeroBolsin": 1001, "opcion": 1}
                """;

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-recepcion")
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
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-bolsin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroBolsin": 1002}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-recepcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroBolsin": 1002, "opcion": 1}
                                """))
                .andExpect(status().isOk());

        String body = """
                {"numeroBolsin": 1002, "opcion": 1, "confirmado": true}
                """;

        mockMvc.perform(post("/api/recepcion-bolsin/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmado").value(true))
                .andExpect(jsonPath("$.numeroBolsin").value(1002))
                .andExpect(jsonPath("$.nuevoEstadoBolsin").value("Recibido en CM destino"))
                .andExpect(jsonPath("$.nuevoEstadoRemitos").value("Recibido y Aceptado"))
                .andExpect(jsonPath("$.nuevoEstadoDocumentaciones").value("Recibida y aceptada"))
                .andExpect(jsonPath("$.remitosActualizados").value(greaterThan(0)))
                .andExpect(jsonPath("$.documentacionesActualizadas").value(greaterThan(0)))
                .andExpect(jsonPath("$.notificaciones", hasSize(greaterThanOrEqualTo(1))));

        Bolsin b1002 = bolsinRepository.findByNumeroBolsin(1002).orElseThrow();
        b1002.getCambioEstado().size();
        assertFalse(b1002.sosEnviado());
        CambioEstadoBolsin ce1002 = b1002.getCambioEstado().stream()
                .filter(CambioEstadoBolsin::esActual)
                .findFirst()
                .orElseThrow();
        assertEquals("Recibido en CM destino", ce1002.getEstado().getNombre());
    }

    // =========================================================================
    // Flujo A6: No confirmar
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("POST /api/recepcion-bolsin/confirmar con confirmado=false (A6) -> no modifica datos")
    void confirmarRecepcion_noConfirmado_noModificaBD() throws Exception {
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-bolsin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroBolsin": 1003}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-recepcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroBolsin": 1003, "opcion": 1}
                                """))
                .andExpect(status().isOk());

        String body = """
                {"numeroBolsin": 1003, "opcion": 1, "confirmado": false}
                """;

        mockMvc.perform(post("/api/recepcion-bolsin/confirmar")
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
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-bolsin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroBolsin": 1001}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-recepcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroBolsin": 1001, "opcion": 1}
                                """))
                .andExpect(status().isOk());

        String body = """
                {"numeroBolsin": 1001, "opcion": 1, "confirmado": true}
                """;

        // Primer envío -> 200
        mockMvc.perform(post("/api/recepcion-bolsin/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmado").value(true));

        // Segundo envío -> 409 (porque finCU reinició el estado y no hay bolsín seleccionado)
        mockMvc.perform(post("/api/recepcion-bolsin/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("Debe seleccionar un bolsín")));
    }

    // =========================================================================
    // Confirmar fuera de secuencia -> 409 Conflict
    // =========================================================================

    @Test
    @DisplayName("POST /api/recepcion-bolsin/confirmar sin haber seleccionado bolsín -> 409 Conflict")
    void confirmarSinSeleccionarBolsin_devuelve409() throws Exception {
        String body = """
                {"numeroBolsin": 1001, "opcion": 1, "confirmado": true}
                """;
        mockMvc.perform(post("/api/recepcion-bolsin/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("Debe seleccionar un bolsín")));
    }

    // =========================================================================
    // Cambio de usuario a mitad del flujo -> reinicia estado y confirmar da 409
    // =========================================================================

    @Test
    @DisplayName("POST /api/sesion/iniciar a mitad del flujo reinicia el estado -> confirmar devuelve 409")
    void cambioUsuarioAMitadDeFlujo_reiniciaEstado_confirmarDevuelve409() throws Exception {
        // 1. Iniciar recepción como Carlos Gómez (Córdoba)
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk());

        // 2. Seleccionar bolsín 1001
        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-bolsin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroBolsin": 1001}
                                """))
                .andExpect(status().isOk());

        // 3. Seleccionar recepción
        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-recepcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroBolsin": 1001, "opcion": 1}
                                """))
                .andExpect(status().isOk());

        // 4. Cambiar usuario a mitad del flujo (POST /api/sesion/iniciar con Laura Torres, id=4)
        mockMvc.perform(post("/api/sesion/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"empleadoId": 4}
                                """))
                .andExpect(status().isOk());

        // 5. Intentar confirmar -> 409 Conflict porque el cambio de sesión llamó a finCU()
        mockMvc.perform(post("/api/recepcion-bolsin/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroBolsin": 1001, "opcion": 1, "confirmado": true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("Debe seleccionar un bolsín")));
    }

    // =========================================================================
    // Filtros A1 y A2 -> 404 Not Found
    // =========================================================================

    @Test
    @DisplayName("GET /api/recepcion-bolsin/bolsines?numeroPrecinto=99999 (A1) -> 404 con mensaje")
    void filtrarBolsines_precintoInexistente_devuelve404() throws Exception {
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/recepcion-bolsin/bolsines")
                        .param("numeroPrecinto", "99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value(containsString("precinto")));
    }

    @Test
    @DisplayName("GET /api/recepcion-bolsin/bolsines?cmOrigen=CM+Inexistente (A2) -> 404 con mensaje")
    void filtrarBolsines_cmOrigenInexistente_devuelve404() throws Exception {
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/recepcion-bolsin/bolsines")
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
        sesionService.login("laura", "1234");
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value(containsString("bolsines enviados pendientes")));
    }

    // =========================================================================
    // Opción no disponible -> 501 Not Implemented
    // =========================================================================

    @Test
    @DisplayName("POST /api/recepcion-bolsin/seleccionar-recepcion opcion=2 -> 501")
    void seleccionarRecepcion_opcion2_devuelve501() throws Exception {
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar")
                        .header("X-Empleado-Id", EMP_CORDOBA))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-bolsin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numeroBolsin": 1001}
                                """))
                .andExpect(status().isOk());

        String body = """
                {"numeroBolsin": 1001, "opcion": 2}
                """;

        mockMvc.perform(post("/api/recepcion-bolsin/seleccionar-recepcion")
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
        mockMvc.perform(post("/api/recepcion-bolsin/cancelar"))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // GET /api/comisiones-medicas
    // =========================================================================

    @Test
    @DisplayName("GET /api/comisiones-medicas -> lista no vacía, ordenada, con campo nombre")
    void listarComisionesMedicas_retornaListaOrdenada() throws Exception {
        mockMvc.perform(get("/api/comisiones-medicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].nombre").isNotEmpty())
                .andExpect(result -> {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    var list = mapper.readTree(result.getResponse().getContentAsString());
                    String primero = list.get(0).get("nombre").asText();
                    String segundo = list.get(1).get("nombre").asText();
                    assertTrue(
                        primero.compareToIgnoreCase(segundo) <= 0,
                        "Las comisiones médicas no están ordenadas alfabéticamente: '" + primero + "' > '" + segundo + "'"
                    );
                });
    }

    // =========================================================================
    // Manejo de errores de conversión y formato (400 Bad Request)
    // =========================================================================

    @Test
    @DisplayName("GET /api/recepcion-bolsin/bolsines?numeroPrecinto=abc -> 400 con mensaje")
    void filtrarBolsines_precintoConLetras_devuelve400() throws Exception {
        mockMvc.perform(get("/api/recepcion-bolsin/bolsines")
                        .param("numeroPrecinto", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El valor ingresado para 'numeroPrecinto' no es válido."));
    }

    @Test
    @DisplayName("GET /api/recepcion-bolsin/bolsines?numeroPrecinto=99999999999 -> 400 con mensaje")
    void filtrarBolsines_precintoEnorme_devuelve400() throws Exception {
        mockMvc.perform(get("/api/recepcion-bolsin/bolsines")
                        .param("numeroPrecinto", "99999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El valor ingresado para 'numeroPrecinto' no es válido."));
    }

    @Test
    @DisplayName("POST /api/recepcion-bolsin/iniciar sin sesión activa -> 400 con mensaje")
    void iniciar_sinSesionActiva_devuelve400() throws Exception {
        sesionService.logout();
        mockMvc.perform(post("/api/recepcion-bolsin/iniciar"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("No hay una sesión activa")));
    }

    @Test
    @DisplayName("POST /api/recepcion-bolsin/confirmar con body vacío -> 400 con mensaje")
    void confirmar_bodyVacio_devuelve400() throws Exception {
        mockMvc.perform(post("/api/recepcion-bolsin/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La solicitud tiene un formato inválido o está vacía."));
    }
}
