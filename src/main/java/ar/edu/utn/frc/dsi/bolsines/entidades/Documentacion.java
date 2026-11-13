package ar.edu.utn.frc.dsi.bolsines.entidades;

import ar.edu.utn.frc.dsi.bolsines.entidades.estado.Estado;
import ar.edu.utn.frc.dsi.bolsines.entidades.estado.EstadoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contexto del Patrón State (GoF).
 *
 * Mantiene la referencia polimórfica a su Estado actual y su colección de historial
 * de cambios de estado. No evalúa condicionales sobre su estado; delega directamente
 * en el objeto Estado asignado.
 */
@Entity
@Table(name = "documentaciones")
public class Documentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int numero;

    @Column(nullable = false, length = 255)
    private String asunto;

    @Column(nullable = false)
    private LocalDateTime fechaPase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cm_origen_id", nullable = false)
    private ComisionMedica cmOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @Convert(converter = EstadoConverter.class)
    @Column(name = "estado", nullable = false, length = 50)
    private Estado estado;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("fechaHoraInicio ASC")
    @JoinColumn(name = "documentacion_id")
    private List<CambioEstadoDocumentacion> cambioEstado = new ArrayList<>();

    public Documentacion() {
    }

    public Documentacion(int numero, String asunto, LocalDateTime fechaPase, ComisionMedica cmOrigen, TipoDocumento tipoDocumento, Estado estado) {
        this.numero = numero;
        this.asunto = asunto;
        this.fechaPase = fechaPase;
        this.cmOrigen = cmOrigen;
        this.tipoDocumento = tipoDocumento;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public LocalDateTime getFechaPase() {
        return fechaPase;
    }

    public void setFechaPase(LocalDateTime fechaPase) {
        this.fechaPase = fechaPase;
    }

    public ComisionMedica getCmOrigen() {
        return cmOrigen;
    }

    public void setCmOrigen(ComisionMedica cmOrigen) {
        this.cmOrigen = cmOrigen;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public List<CambioEstadoDocumentacion> getCambioEstado() {
        return cambioEstado;
    }

    public void setCambioEstado(List<CambioEstadoDocumentacion> cambioEstado) {
        this.cambioEstado = cambioEstado;
    }

    // =========================================================================
    // Métodos del dominio / Patrón State y Patrón Experto (diagramas E1 y E2)
    // =========================================================================

    /**
     * CU 28 – Paso 11c: Transición polimórfica sin estructuras condicionales (if/switch).
     *
     * La Documentación delega el mensaje 'aceptar' a su objeto Estado actual, pasando
     * 'this' para que el estado concrete la transición y actualice la referencia.
     */
    public void aceptar(LocalDateTime fechaHoraActual, Empleado responsable) {
        this.estado.aceptar(this, fechaHoraActual, responsable, this.cambioEstado);
    }

    /**
     * Transición polimórfica para Flujo A4 (Rechazar).
     */
    public void rechazar(LocalDateTime fechaHoraActual, Empleado responsable) {
        this.estado.rechazar(this, fechaHoraActual, responsable, this.cambioEstado);
    }

    /**
     * Transición polimórfica para Flujo A5 (Redirigir).
     */
    public void redirigir(LocalDateTime fechaHoraActual, Empleado responsable) {
        this.estado.redirigir(this, fechaHoraActual, responsable, this.cambioEstado);
    }

    /**
     * Transición polimórfica para Flujo A3 (No recepción).
     */
    public void registrarNoRecepcion(LocalDateTime fechaHoraActual, Empleado responsable) {
        this.estado.registrarNoRecepcion(this, fechaHoraActual, responsable, this.cambioEstado);
    }

    /**
     * Agrega un nuevo registro al historial de cambios de estado.
     */
    public void agregarCambioEstado(CambioEstadoDocumentacion nuevoCambio) {
        this.cambioEstado.add(nuevoCambio);
    }

    /**
     * Retorna el nombre del tipo de documento asociado.
     * Corresponde al mensaje buscarTipoDocumentacion() en el diagrama de secuencia.
     */
    public String buscarTipoDocumentacion() {
        return this.tipoDocumento != null ? this.tipoDocumento.getNombre() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Documentacion that = (Documentacion) o;
        return numero == that.numero || (id != null && Objects.equals(id, that.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return "Doc #" + numero + " - " + asunto;
    }
}

