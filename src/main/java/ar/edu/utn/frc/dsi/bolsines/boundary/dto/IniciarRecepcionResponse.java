package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

import java.util.List;

public record IniciarRecepcionResponse(
        String cmUsuario,
        List<BolsinResumenDto> bolsines
) {
}

