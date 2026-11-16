package ar.edu.utn.frc.dsi.bolsines.boundary;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.LoginRequest;
import ar.edu.utn.frc.dsi.bolsines.boundary.dto.SesionDto;
import ar.edu.utn.frc.dsi.bolsines.control.SesionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<SesionDto> getSesion() {
        return ResponseEntity.ok(sesionService.getSesionActiva());
    }
}
