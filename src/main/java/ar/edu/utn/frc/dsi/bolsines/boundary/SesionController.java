package ar.edu.utn.frc.dsi.bolsines.boundary;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.CambiarSesionRequest;
import ar.edu.utn.frc.dsi.bolsines.boundary.dto.LoginRequest;
import ar.edu.utn.frc.dsi.bolsines.boundary.dto.SesionDto;
import ar.edu.utn.frc.dsi.bolsines.control.SesionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SesionController – Boundary para inicio y fin de sesión (login/logout).
 *
 * Delega exclusivamente a SesionService, no al GestorRecepcionBolsin.
 */
@RestController
@RequestMapping("/api/sesion")
public class SesionController {

    private final SesionService sesionService;

    public SesionController(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    /**
     * POST /api/sesion/login
     * Valida usuario y contraseña y crea una sesión activa.
     */
    @PostMapping("/login")
    public ResponseEntity<SesionDto> login(@RequestBody LoginRequest request) {
        SesionDto sesion = sesionService.login(request.usuario(), request.password());
        return ResponseEntity.ok(sesion);
    }

    /**
     * POST /api/sesion/logout
     * Cierra la sesión activa seteando fechaHoraFin.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        sesionService.logout();
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/sesion
     * Obtiene los datos de la sesión activa actual.
     */
    @GetMapping
    public ResponseEntity<SesionDto> getSesion(@RequestParam(required = false) Long empleadoId) {
        SesionDto sesion;
        try {
            sesion = sesionService.getSesionActiva();
        } catch (Exception ex) {
            if (empleadoId != null) {
                sesion = sesionService.iniciarSesionPorEmpleado(empleadoId);
            } else {
                throw ex;
            }
        }
        return ResponseEntity.ok(sesion);
    }

    /**
     * GET /api/sesion/empleados
     * Retorna el listado de empleados disponibles.
     */
    @GetMapping("/empleados")
    public ResponseEntity<List<SesionDto>> listarEmpleados() {
        return ResponseEntity.ok(sesionService.listarEmpleados());
    }

    /**
     * POST /api/sesion/iniciar
     * Cambia la sesión activa por id de empleado (compatibilidad).
     */
    @PostMapping("/iniciar")
    public ResponseEntity<SesionDto> iniciarSesion(@RequestBody CambiarSesionRequest request) {
        SesionDto sesion = sesionService.iniciarSesionPorEmpleado(request.empleadoId());
        return ResponseEntity.ok(sesion);
    }
}
