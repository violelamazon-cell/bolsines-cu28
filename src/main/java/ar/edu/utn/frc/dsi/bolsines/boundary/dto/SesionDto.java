package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

public record SesionDto(
        Long empleadoId,
        String nombreCompleto,
        String email,
        String codigoCm,
        String nombreCm) {
    public SesionDto(Long empleadoId, String nombreCompleto, String nombreCm, String codigoCm) {
        this(empleadoId, nombreCompleto, null, codigoCm, nombreCm);
    }
}
