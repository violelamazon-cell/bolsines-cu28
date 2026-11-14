package ar.edu.utn.frc.dsi.bolsines.boundary;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.ComisionMedicaDto;
import ar.edu.utn.frc.dsi.bolsines.control.GestorRecepcionBolsin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ComisionMedicaController – expone el catálogo de Comisiones Médicas
 * para uso del frontend (desplegable de filtro de CM origen en el Paso 1).
 *
 * Responsabilidad ÚNICA: recibir la petición HTTP, delegar al Gestor y devolver el DTO.
 */
@RestController
@RequestMapping("/api/comisiones-medicas")
public class ComisionMedicaController {

    private final GestorRecepcionBolsin gestor;

    public ComisionMedicaController(GestorRecepcionBolsin gestor) {
        this.gestor = gestor;
    }

    /**
     * Lista los nombres de todas las Comisiones Médicas, ordenadas alfabéticamente.
     *
     * GET /api/comisiones-medicas
     * Respuesta 200: [ { "nombre": "CM Córdoba" }, ... ]
     */
    @GetMapping
    public ResponseEntity<List<ComisionMedicaDto>> listarComisionesMedicas() {
        return ResponseEntity.ok(gestor.listarComisionesMedicas());
    }
}

