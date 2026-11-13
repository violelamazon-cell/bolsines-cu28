package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

import java.util.List;

public record SeleccionarBolsinResponse(
        int numeroBolsin,
        int numeroPrecinto,
        Double peso,
        String cmOrigen,
        List<RemitoDto> remitos,
        List<OpcionRecepcionDto> opcionesRecepcion
) {
}

