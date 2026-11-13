package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

public record ConfirmarRecepcionRequest(
        int numeroBolsin,
        int opcion,
        boolean confirmado
) {
}

