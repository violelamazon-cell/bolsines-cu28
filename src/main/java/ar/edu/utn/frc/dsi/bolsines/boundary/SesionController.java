package ar.edu.utn.frc.dsi.bolsines.boundary;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.CambiarSesionRequest;
import ar.edu.utn.frc.dsi.bolsines.boundary.dto.SesionDto;
import ar.edu.utn.frc.dsi.bolsines.control.GestorRecepcionBolsin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SesionController – Boundary para gestión de sesión simulada.
 *
 * Facilita el selector de 'Usuario activo' en el frontend para probar
 * los distintos roles y estados vacíos sin requerir autenticación real.
 */
@RestController
@RequestMapping("/api/sesion")
public class SesionController {

    private final GestorRecepcionBolsin gestor;

    public SesionController(GestorRecepcionBolsin gestor) {
        this.gestor = gestor;
    }

    /**
     * GET /api/sesion?empleadoId={id}
     * Obtiene los datos del empleado activo (por defecto id=1, Carlos Gómez).
     */
    @GetMapping
    public ResponseEntity<SesionDto> getSesion(@RequestParam(defaultValue = "1") Long empleadoId) {
        SesionDto sesion = gestor.getSesion(empleadoId);
        return ResponseEntity.ok(sesion);
    }

    /**
     * GET /api/sesion/empleados
     * Retorna el listado completo de empleados para el selector del frontend.
     */
    @GetMapping("/empleados")
    public ResponseEntity<List<SesionDto>> listarEmpleados() {
        return ResponseEntity.ok(gestor.listarEmpleados());
    }

    /**
     * POST /api/sesion/iniciar
     * Cambia el empleado activo en el frontend simulado.
     */
    @PostMapping("/iniciar")
    public ResponseEntity<SesionDto> iniciarSesion(@RequestBody CambiarSesionRequest request) {
        SesionDto sesion = gestor.getSesion(request.empleadoId());
        return ResponseEntity.ok(sesion);
    }
}

