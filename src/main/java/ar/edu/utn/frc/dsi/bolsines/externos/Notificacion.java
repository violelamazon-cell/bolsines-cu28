package ar.edu.utn.frc.dsi.bolsines.externos;

import java.time.LocalDateTime;

/**
 * Representa una notificación emitida por el Caso de Uso 29.
 */
public record Notificacion(
        String codigoCmDestino,
        String nombreCmDestino,
        String emailDestinatario,
        String asunto,
        String cuerpo,
        LocalDateTime fechaHoraEnvio
) {
}

