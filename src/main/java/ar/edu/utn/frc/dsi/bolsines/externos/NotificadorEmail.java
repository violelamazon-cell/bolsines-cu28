package ar.edu.utn.frc.dsi.bolsines.externos;

import ar.edu.utn.frc.dsi.bolsines.entidades.ComisionMedica;

/**
 * Interfaz de comunicación con el servicio de correo electrónico (CU 29 Notificar recepción de bolsín).
 */
public interface NotificadorEmail {

    /**
     * Envía un correo electrónico a la Comisión Médica indicada.
     *
     * @param destinataria Comisión Médica receptora del correo.
     * @param asunto       Asunto del correo.
     * @param cuerpo       Cuerpo del correo con el detalle de la recepción.
     * @return Notificacion con los datos del envío.
     */
    Notificacion enviar(ComisionMedica destinataria, String asunto, String cuerpo);
}

