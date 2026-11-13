package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

import ar.edu.utn.frc.dsi.bolsines.entidades.CambioEstadoDocumentacion;
import ar.edu.utn.frc.dsi.bolsines.entidades.Documentacion;
import ar.edu.utn.frc.dsi.bolsines.entidades.Empleado;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Clase base abstracta del Patrón State (GoF) para el ciclo de vida de Documentacion.
 *
 * Cada estado concreto sobrescribe únicamente las transiciones válidas para su contexto.
 * Por defecto, cualquier transición no permitida lanza una excepción IllegalStateException.
 */
public abstract class Estado {

    private final String nombre;
    private final String descripcion;
    private final String ambito;

    public Estado(String nombre, String descripcion, String ambito) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.ambito = ambito;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getAmbito() {
        return ambito;
    }

    // =========================================================================
    // Métodos de transición de la Máquina de Estados (CU 28 y otros casos de uso)
    // =========================================================================

    /**
     * Transición CU 28: Aceptar la documentación recibida en la CM destino.
     */
    public void aceptar(Documentacion doc, LocalDateTime ahora, Empleado resp, List<CambioEstadoDocumentacion> cambios) {
        throw transicionNoPermitida("aceptar");
    }

    /**
     * Transición CU 28 (A4): Rechazar documentación que no corresponde a la CM destino.
     */
    public void rechazar(Documentacion doc, LocalDateTime ahora, Empleado resp, List<CambioEstadoDocumentacion> cambios) {
        throw transicionNoPermitida("rechazar");
    }

    /**
     * Transición CU 28 (A5): Redirigir documentación a otra área (en CMC).
     */
    public void redirigir(Documentacion doc, LocalDateTime ahora, Empleado resp, List<CambioEstadoDocumentacion> cambios) {
        throw transicionNoPermitida("redirigir");
    }

    /**
     * Transición CU 28 (A3): Registrar no recepción de documentación faltante en el bolsín.
     */
    public void registrarNoRecepcion(Documentacion doc, LocalDateTime ahora, Empleado resp, List<CambioEstadoDocumentacion> cambios) {
        throw transicionNoPermitida("registrarNoRecepcion");
    }

    // Transiciones de otros casos de uso (definidas en la máquina de estados pero fuera del CU 28)
    public void cancelar() {
        throw transicionNoPermitida("cancelar");
    }

    public void remitar() {
        throw transicionNoPermitida("remitar");
    }

    public void darDeBaja() {
        throw transicionNoPermitida("darDeBaja");
    }

    public void asociarBolsin() {
        throw transicionNoPermitida("asociarBolsin");
    }

    public void quitarDeBolsin() {
        throw transicionNoPermitida("quitarDeBolsin");
    }

    public void revincular() {
        throw transicionNoPermitida("revincular");
    }

    public void retirarBolsin() {
        throw transicionNoPermitida("retirarBolsin");
    }

    protected IllegalStateException transicionNoPermitida(String nombreMetodo) {
        return new IllegalStateException(
                "Transición '" + nombreMetodo + "' no permitida desde el estado '" + nombre + "'."
        );
    }

    // =========================================================================
    // equals / hashCode basados exclusivamente en el nombre canónico del estado
    // Esto garantiza que Hibernate no detecte 'dirty changes' falsos en los flushes.
    // =========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Estado estado = (Estado) o;
        return Objects.equals(nombre != null ? nombre.trim().toLowerCase() : null,
                              estado.nombre != null ? estado.nombre.trim().toLowerCase() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre != null ? nombre.trim().toLowerCase() : null);
    }

    @Override
    public String toString() {
        return nombre;
    }
}

