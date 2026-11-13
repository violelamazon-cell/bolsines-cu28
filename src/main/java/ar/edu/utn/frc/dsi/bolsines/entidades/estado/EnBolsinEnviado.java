package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

import ar.edu.utn.frc.dsi.bolsines.entidades.CambioEstadoDocumentacion;
import ar.edu.utn.frc.dsi.bolsines.entidades.Documentacion;
import ar.edu.utn.frc.dsi.bolsines.entidades.Empleado;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado concreto del Patrón State: "En bolsín enviado".
 *
 * Representa la documentación que se encuentra viajando en un bolsín cerrado y retirado por el correo.
 * Es el estado origen del flujo principal del Caso de Uso 28 (Registrar recepción de bolsín).
 */
public class EnBolsinEnviado extends Estado {

    public static final String NOMBRE = "En bolsín enviado";

    public EnBolsinEnviado() {
        super(NOMBRE, "Documentación en tránsito en un bolsín enviado", "Documentacion");
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
    public void aceptar(Documentacion doc, LocalDateTime ahora, Empleado resp, List<CambioEstadoDocumentacion> cambios) {
        // 1. actual = buscarEstadoActual(cambios)
        CambioEstadoDocumentacion actual = buscarEstadoActual(cambios);

        // 2. actual.setFechaHoraFin(ahora)
        if (actual != null) {
            actual.setFechaHoraFin(ahora);
        }

        // 3. nuevo = crearEstado()
        Estado nuevo = crearEstado();

        // 4. cambioNuevo = crearCambioEstado(ahora, resp, nuevo)
        CambioEstadoDocumentacion cambioNuevo = crearCambioEstado(ahora, resp, nuevo);

        // 5. doc.setEstado(nuevo)
        doc.setEstado(nuevo);

        // 6. doc.agregarCambioEstado(cambioNuevo)
        doc.agregarCambioEstado(cambioNuevo);
    }

    // =========================================================================
    // Métodos privados modelados en el diagrama de clases y secuencia de E2
    // =========================================================================

    private CambioEstadoDocumentacion buscarEstadoActual(List<CambioEstadoDocumentacion> cambios) {
        if (cambios == null) return null;
        for (CambioEstadoDocumentacion c : cambios) {
            if (c.esActual()) {
                return c;
            }
        }
        return null;
    }

    private Estado crearEstado() {
        return new RecibidaYAceptada();
    }

    private CambioEstadoDocumentacion crearCambioEstado(LocalDateTime ahora, Empleado resp, Estado nuevo) {
        return new CambioEstadoDocumentacion(ahora, null, nuevo, resp);
    }
}

