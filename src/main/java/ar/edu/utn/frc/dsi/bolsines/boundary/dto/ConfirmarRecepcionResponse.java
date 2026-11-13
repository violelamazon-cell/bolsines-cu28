package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

import java.util.List;

public record ConfirmarRecepcionResponse(
        boolean confirmado,
        String mensaje,
        Integer numeroBolsin,
        String nuevoEstadoBolsin,
        int remitosActualizados,
        int documentacionesActualizadas,
        List<NotificacionDto> notificaciones
) {
}

