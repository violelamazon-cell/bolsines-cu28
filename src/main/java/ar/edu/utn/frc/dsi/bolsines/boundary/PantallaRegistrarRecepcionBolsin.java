package ar.edu.utn.frc.dsi.bolsines.boundary;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.*;
import ar.edu.utn.frc.dsi.bolsines.control.ConsultasService;
import ar.edu.utn.frc.dsi.bolsines.control.GestorRecepcionBolsin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PantallaRegistrarRecepcionBolsin – boundary del CU 28 (diagramas E1 y E2).
 *
 * Responsabilidad ÚNICA: recibir las peticiones HTTP, delegar al Gestor con estado y devolver
 * las respuestas DTO. NUNCA contiene lógica de dominio ni de negocio.
 *
 * Desviación D3: los métodos de salida del diagrama (mostrar... / solicitar...) se reemplazan
 * por retornos DTO en la API REST.
 */
@RestController
@RequestMapping("/api/recepcion-bolsin")
public class PantallaRegistrarRecepcionBolsin {

    private final GestorRecepcionBolsin gestor;
    private final ConsultasService consultasService;

    public PantallaRegistrarRecepcionBolsin(GestorRecepcionBolsin gestor, ConsultasService consultasService) {
        this.gestor = gestor;
        this.consultasService = consultasService;
    }

    /**
     * CU 28 Pasos 1 a 4: Habilitar pantalla y listar bolsines enviados para la CM del usuario.
     * Corresponde a opcRegistrarRecepcionBolsin() + habilitarPantalla() del diagrama.
     *
     * POST /api/recepcion-bolsin/iniciar
     */
    @PostMapping("/iniciar")
    public ResponseEntity<IniciarRecepcionResponse> opcRegistrarRecepcionBolsin() {
        IniciarRecepcionResponse respuesta = gestor.registrarRecepcionBolsin();
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Consulta de solo lectura: retorna la cantidad de bolsines pendientes
     * para la Comisión Médica del usuario logueado, sin iniciar el CU ni alterar el Gestor.
     *
     * GET /api/recepcion-bolsin/pendientes
     */
    @GetMapping("/pendientes")
    public ResponseEntity<BolsinesPendientesDto> obtenerCantidadPendientes() {
        int cantidad = consultasService.contarBolsinesPendientes();
        return ResponseEntity.ok(new BolsinesPendientesDto(cantidad));
    }

    /**
     * CU 28 Paso 4: Filtros de búsqueda (A1 y A2).
     *
     * GET /api/recepcion-bolsin/bolsines?numeroPrecinto=&cmOrigen=
     */
    @GetMapping("/bolsines")
    public ResponseEntity<List<BolsinResumenDto>> habilitarPantalla(
            @RequestParam(required = false) Integer numeroPrecinto,
            @RequestParam(name = "cmOrigen", required = false) String cmOrigen) {
        List<BolsinResumenDto> bolsines = consultasService.filtrarBolsines(numeroPrecinto, cmOrigen);
        return ResponseEntity.ok(bolsines);
    }

    /**
     * CU 28 Pasos 5 a 7: Seleccionar bolsín y obtener detalle de remitos y opciones de recepción.
     * Corresponde a tomarSeleccionBolsin() del diagrama.
     *
     * POST /api/recepcion-bolsin/seleccionar-bolsin
     * Body: { numeroBolsin }
     */
    @PostMapping("/seleccionar-bolsin")
    public ResponseEntity<SeleccionarBolsinResponse> tomarSeleccionBolsin(
            @RequestBody SeleccionarBolsinRequest request) {
        SeleccionarBolsinResponse respuesta = gestor.tomarSeleccionBolsin(request.numeroBolsin());
        return ResponseEntity.ok(respuesta);
    }

    /**
     * CU 28 Pasos 8 y 9: Seleccionar opción de recepción.
     * Corresponde a tomarSeleccionRecepcion() del diagrama.
     *
     * POST /api/recepcion-bolsin/seleccionar-recepcion
     * Body: { numeroBolsin, opcion }
     */
    @PostMapping("/seleccionar-recepcion")
    public ResponseEntity<SeleccionarRecepcionResponse> tomarSeleccionRecepcion(
            @RequestBody SeleccionarRecepcionRequest request) {
        SeleccionarRecepcionResponse respuesta = gestor.tomarSeleccionRecepcion(request.opcion());
        return ResponseEntity.ok(respuesta);
    }

    /**
     * CU 28 Pasos 10 a 12: Confirmar o rechazar recepción.
     * Corresponde a tomarConfirmacion() del diagrama.
     *
     * POST /api/recepcion-bolsin/confirmar
     * Body: { numeroBolsin, opcion, confirmado }
     */
    @PostMapping("/confirmar")
    public ResponseEntity<ConfirmarRecepcionResponse> tomarConfirmacion(
            @RequestBody ConfirmarRecepcionRequest request) {
        ConfirmarRecepcionResponse respuesta = gestor.tomarConfirmacion(request.confirmado());
        return ResponseEntity.ok(respuesta);
    }

    /**
     * CU 28 Observación 1: Cancelar proceso de recepción.
     * Corresponde a finCU() del diagrama.
     *
     * POST /api/recepcion-bolsin/cancelar
     * Respuesta 204: No Content.
     */
    @PostMapping("/cancelar")
    public ResponseEntity<Void> cancelar() {
        gestor.finCU();
        return ResponseEntity.noContent().build();
    }

}
