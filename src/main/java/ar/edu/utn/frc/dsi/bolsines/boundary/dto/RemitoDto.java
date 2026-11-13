package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

import java.util.List;

public record RemitoDto(
        int numeroRemito,
        List<DocumentacionDto> documentaciones
) {
}

