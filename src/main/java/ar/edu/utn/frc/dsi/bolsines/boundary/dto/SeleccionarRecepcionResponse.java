package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

public record SeleccionarRecepcionResponse(
        int opcion,
        String descripcion,
        String resumen,
        boolean requiereConfirmacion
) {
}

