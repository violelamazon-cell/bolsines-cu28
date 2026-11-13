package ar.edu.utn.frc.dsi.bolsines.boundary;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.*;
import ar.edu.utn.frc.dsi.bolsines.control.GestorRecepcionBolsin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PantallaRegistrarRecepcionBolsin – boundary del CU 28 (diagramas E1 y E2).
 *
 * Responsabilidad ÚNICA: recibir las peticiones HTTP, delegar al Gestor y devolver
 * las respuestas DTO. NUNCA contiene lógica de dominio ni de negocio.
 *
 * El empleado logueado se identifica a través del header HTTP 'X-Empleado-Id' (por defecto 1 = Carlos Gómez, Córdoba).
 * Desviación D3: los métodos de salida del diagrama (mostrar... / solicitar...) se reemplazan
 * por retornos DTO en la API REST.
 */
@RestController
@RequestMapping("/api/recepcion-bolsin")
public class PantallaRegistrarRecepcionBolsin {

    private final GestorRecepcionBolsin gestor;

    public PantallaRegistrarRecepcionBolsin(GestorRecepcionBolsin gestor) {
        this.gestor = gestor;
    }

    /**
     * CU 28 Pasos 1 a 4: Habilitar pantalla y listar bolsines enviados para la CM del usuario.
     * Corresponde a opcRegistrarRecepcionBolsin() + habilitarPantalla() del diagrama.
     *
     * POST /api/recepcion-bolsin/iniciar
     * Header: X-Empleado-Id: {id}
     * Respuesta 200: { cmUsuario, codigoCm, bolsines: [...] }
     * Respuesta 404: Si no existen bolsines pendientes para la CM destino.
     */
    @PostMapping("/iniciar")
    public ResponseEntity<IniciarRecepcionResponse> opcRegistrarRecepcionBolsin(
            @RequestHeader(value = "X-Empleado-Id", defaultValue = "1") Long empleadoId) {
        IniciarRecepcionResponse respuesta = gestor.registrarRecepcionBolsin(empleadoId);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * CU 28 Paso 4: Filtros de búsqueda (A1 y A2).
     *
     * GET /api/recepcion-bolsin/bolsines?numeroPrecinto=&cmOrigen=
     * Header: X-Empleado-Id: {id}
     * Respuesta 200: Lista filtrada de bolsines enviados.
     * Respuesta 404 (A1): No se encontró bolsín con el precinto indicado.
     * Respuesta 404 (A2): No se encontró bolsín con la CM de origen indicada.
     */
    @GetMapping("/bolsines")
    public ResponseEntity<List<BolsinResumenDto>> habilitarPantalla(
            @RequestHeader(value = "X-Empleado-Id", defaultValue = "1") Long empleadoId,
            @RequestParam(required = false) Integer numeroPrecinto,
            @RequestParam(name = "cmOrigen", required = false) String cmOrigen) {
        List<BolsinResumenDto> bolsines = gestor.filtrarBolsines(empleadoId, numeroPrecinto, cmOrigen);
        return ResponseEntity.ok(bolsines);
    }

    /**
     * CU 28 Pasos 5 a 7: Seleccionar bolsín y obtener detalle de remitos y opciones de recepción.
     * Corresponde a tomarSeleccionBolsin() del diagrama.
     *
     * POST /api/recepcion-bolsin/seleccionar-bolsin
     * Header: X-Empleado-Id: {id}
     * Body: { numeroBolsin }
     */
    @PostMapping("/seleccionar-bolsin")
    public ResponseEntity<SeleccionarBolsinResponse> tomarSeleccionBolsin(
            @RequestHeader(value = "X-Empleado-Id", defaultValue = "1") Long empleadoId,
            @RequestBody SeleccionarBolsinRequest request) {
        SeleccionarBolsinResponse respuesta = gestor.tomarSeleccionBolsin(empleadoId, request.numeroBolsin());
        return ResponseEntity.ok(respuesta);
    }

    /**
     * CU 28 Pasos 8 y 9: Seleccionar opción de recepción.
     * Corresponde a tomarSeleccionRecepcion() del diagrama.
     *
     * POST /api/recepcion-bolsin/seleccionar-recepcion
     * Header: X-Empleado-Id: {id}
     * Body: { numeroBolsin, opcion }
     * Respuesta 501: Si se seleccionan opciones 2, 3 o 4 (no disponibles todavía).
     */
    @PostMapping("/seleccionar-recepcion")
    public ResponseEntity<SeleccionarRecepcionResponse> tomarSeleccionRecepcion(
            @RequestHeader(value = "X-Empleado-Id", defaultValue = "1") Long empleadoId,
            @RequestBody SeleccionarRecepcionRequest request) {
        SeleccionarRecepcionResponse respuesta = gestor.tomarSeleccionRecepcion(
                empleadoId, request.numeroBolsin(), request.opcion()
        );
        return ResponseEntity.ok(respuesta);
    }

    /**
     * CU 28 Pasos 10 a 12: Confirmar o rechazar recepción.
     * Corresponde a tomarConfirmacion() del diagrama.
     *
     * POST /api/recepcion-bolsin/confirmar
     * Header: X-Empleado-Id: {id}
     * Body: { numeroBolsin, opcion, confirmado }
     * Respuesta 200: Resultado de la confirmación (o de la no-confirmación A6).
     * Respuesta 409: Conflicto si el bolsín ya fue recibido.
     */
    @PostMapping("/confirmar")
    public ResponseEntity<ConfirmarRecepcionResponse> tomarConfirmacion(
            @RequestHeader(value = "X-Empleado-Id", defaultValue = "1") Long empleadoId,
            @RequestBody ConfirmarRecepcionRequest request) {
        ConfirmarRecepcionResponse respuesta = gestor.tomarConfirmacion(
                empleadoId,
                request.numeroBolsin(),
                request.opcion(),
                request.confirmado()
        );
        return ResponseEntity.ok(respuesta);
    }

    /**
     * CU 28 Observación 1: Cancelar proceso de recepción.
     * Corresponde a cancelar() del diagrama.
     *
     * POST /api/recepcion-bolsin/cancelar
     * Respuesta 204: No Content.
     */
    @PostMapping("/cancelar")
    public ResponseEntity<Void> cancelar() {
        gestor.cancelar();
        return ResponseEntity.noContent().build();
    }
}

