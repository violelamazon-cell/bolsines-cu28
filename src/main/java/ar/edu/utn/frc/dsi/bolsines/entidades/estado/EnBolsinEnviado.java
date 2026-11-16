package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

import ar.edu.utn.frc.dsi.bolsines.entidades.CambioEstadoDocumentacion;
import ar.edu.utn.frc.dsi.bolsines.entidades.Documentacion;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado concreto del Patrón State: "En bolsín enviado".
 *
 * Representa la documentación que se encuentra viajando en un bolsín cerrado y retirado por el correo.
 * Es el estado origen del flujo principal del Caso de Uso 28 (Registrar recepción de bolsín).
 */
public class EnBolsinEnviado extends Estado {

    public EnBolsinEnviado() {
    }

    /**
     * CU 28 – Paso 11c (Patrón State).
     *
     * Ejecuta la secuencia exacta modelada en el diagrama de secuencia E2:
     * 1. actual = buscarEstadoActual(cambios)
     * 2. actual.setFechaHoraFin(ahora)
     * 3. nuevo = crearEstado()  [new RecibidaYAceptada()]
     * 4. cambioNuevo = crearCambioEstado(ahora, resp, nuevo)
     * 5. doc.setEstado(nuevo)
     * 6. doc.agregarCambioEstado(cambioNuevo)
     */
    @Override
    public void aceptar(Documentacion doc, LocalDateTime ahora, String[] resp, List<CambioEstadoDocumentacion> cambios) {
        CambioEstadoDocumentacion actual = buscarEstadoActual(cambios);
        if (actual != null) {
            actual.setFechaHoraFin(ahora);
        }
        Estado nuevo = crearEstado();
        CambioEstadoDocumentacion cambioNuevo = crearCambioEstado(ahora, resp, nuevo);
        doc.setEstado(nuevo);
        doc.agregarCambioEstado(cambioNuevo);
    }

    @Override
    public void rechazar() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    @Override
    public void redirigir() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    @Override
    public void registrarNoRecepcion() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    // =========================================================================
    // Métodos de apoyo públicos modelados en el diagrama de clases y secuencia E2
    // =========================================================================

    public CambioEstadoDocumentacion buscarEstadoActual(List<CambioEstadoDocumentacion> cambios) {
        if (cambios == null) return null;
        for (CambioEstadoDocumentacion c : cambios) {
            if (c.esEstadoActual()) {
                return c;
            }
        }
        return null;
    }

    public Estado crearEstado() {
        return new RecibidaYAceptada();
    }

    public CambioEstadoDocumentacion crearCambioEstado(LocalDateTime ahora, String[] resp, Estado nuevo) {
        return new CambioEstadoDocumentacion(ahora, resp, nuevo);
    }
}
