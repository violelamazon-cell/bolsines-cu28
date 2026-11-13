package ar.edu.utn.frc.dsi.bolsines.externos;

import ar.edu.utn.frc.dsi.bolsines.entidades.ComisionMedica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementación simulada del servicio de envío de correos (CU 29).
 *
 * Registra los envíos en el log del sistema y mantiene una lista en memoria
 * para auditoría e inspección en la interfaz de usuario.
 */
@Component
public class NotificadorEmailSimulado implements NotificadorEmail {

    private static final Logger log = LoggerFactory.getLogger(NotificadorEmailSimulado.class);

    private final List<Notificacion> historialNotificaciones = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Notificacion enviar(ComisionMedica destinataria, String asunto, String cuerpo) {
        LocalDateTime ahora = LocalDateTime.now();
        String email = destinataria != null ? destinataria.getEmail() : "sin-email@bolsines.gov.ar";
        String codigoCm = destinataria != null ? destinataria.getCodigo() : "N/D";
        String nombreCm = destinataria != null ? destinataria.getNombre() : "Comisión Desconocida";

        Notificacion notificacion = new Notificacion(
                codigoCm,
                nombreCm,
                email,
                asunto,
                cuerpo,
                ahora
        );

        historialNotificaciones.add(notificacion);

        log.info("CU 29 [Email Simulado] Enviado a: {} <{}> | Asunto: {} | Fecha: {}",
                nombreCm, email, asunto, ahora);

        return notificacion;
    }

    public List<Notificacion> getHistorialNotificaciones() {
        return Collections.unmodifiableList(new ArrayList<>(historialNotificaciones));
    }

    public void limpiarHistorial() {
        historialNotificaciones.clear();
    }
}

