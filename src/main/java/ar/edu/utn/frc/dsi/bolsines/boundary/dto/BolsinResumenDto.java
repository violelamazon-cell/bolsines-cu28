package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

import java.time.LocalDateTime;

public record BolsinResumenDto(
        int numeroBolsin,
        int numeroPrecinto,
        Double peso,
        String cmOrigen,
        LocalDateTime fechaEnvio
) {
}

