package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

public record SesionDto(
        Long empleadoId,
        String nombreCompleto,
        String email,
        String codigoCm,
        String nombreCm
) {
}

