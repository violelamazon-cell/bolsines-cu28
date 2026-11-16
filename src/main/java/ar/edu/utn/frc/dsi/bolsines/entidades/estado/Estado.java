package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

import ar.edu.utn.frc.dsi.bolsines.entidades.CambioEstadoDocumentacion;
import ar.edu.utn.frc.dsi.bolsines.entidades.Documentacion;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Clase base abstracta del Patrón State (GoF) para el ciclo de vida de Documentacion.
 *
 * En el diagrama de clases E2 no posee atributos de estado propios.
 * Las transiciones no implementadas o fuera del alcance lanzan UnsupportedOperationException.
 */
public abstract class Estado {

    public Estado() {
    }

    // =========================================================================
    // Métodos de transición de la Máquina de Estados (diagrama de clases E2)
    // =========================================================================

    public void aceptar(Documentacion doc, LocalDateTime ahora, String[] resp, List<CambioEstadoDocumentacion> cambios) {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    public void rechazar() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    public void redirigir() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    public void registrarNoRecepcion() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    public void cancelar() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    public void remitar() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    public void darDeBaja() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    public void asociarBolsin() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    public void quitarDeBolsin() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    public void revincular() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    public void retirarBolsin() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    // =========================================================================
    // equals / hashCode / toString basados exclusivamente en la clase concreta
    // =========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
