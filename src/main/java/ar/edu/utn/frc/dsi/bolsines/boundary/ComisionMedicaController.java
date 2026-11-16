package ar.edu.utn.frc.dsi.bolsines.boundary;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.ComisionMedicaDto;
import ar.edu.utn.frc.dsi.bolsines.control.ConsultasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ComisionMedicaController – expone el catálogo de Comisiones Médicas
 * para uso del frontend (desplegable de filtro de CM origen en el Paso 1).
 *
 * Responsabilidad ÚNICA: recibir la petición HTTP, delegar a ConsultasService y devolver el DTO.
 */
@RestController
@RequestMapping("/api/comisiones-medicas")
public class ComisionMedicaController {

    private final ConsultasService consultasService;

    public ComisionMedicaController(ConsultasService consultasService) {
        this.consultasService = consultasService;
    }

    /**
     * Lista los nombres de todas las Comisiones Médicas, ordenadas alfabéticamente.
     *
     * GET /api/comisiones-medicas
     * Respuesta 200: [ { "nombre": "CM Córdoba" }, ... ]
     */
    @GetMapping
    public ResponseEntity<List<ComisionMedicaDto>> listarComisionesMedicas() {
        return ResponseEntity.ok(consultasService.listarComisionesMedicas());
    }
}
