package ar.edu.utn.frc.dsi.bolsines.entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "remitos")
public class Remito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int numero;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cm_origen_id", nullable = false)
    private ComisionMedica origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cm_destino_id", nullable = false)
    private ComisionMedica destino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_remito_id", nullable = false)
    private EstadoRemito estado;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "remito_id")
    private List<DetalleRemito> detalleRemito = new ArrayList<>();

    public Remito() {
    }

    public Remito(int numero, LocalDateTime fecha, ComisionMedica origen, ComisionMedica destino, EstadoRemito estado) {
        this.numero = numero;
        this.fecha = fecha;
        this.origen = origen;
        this.destino = destino;
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

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public ComisionMedica getOrigen() {
        return origen;
    }

    public void setOrigen(ComisionMedica origen) {
        this.origen = origen;
    }

    public ComisionMedica getDestino() {
        return destino;
    }

    public void setDestino(ComisionMedica destino) {
        this.destino = destino;
    }

    public EstadoRemito getEstado() {
        return estado;
    }

    public void setEstado(EstadoRemito estado) {
        this.estado = estado;
    }

    public List<DetalleRemito> getDetalleRemito() {
        return detalleRemito;
    }

    public void setDetalleRemito(List<DetalleRemito> detalleRemito) {
        this.detalleRemito = detalleRemito;
    }

    public void agregarDetalle(DetalleRemito detalle) {
        this.detalleRemito.add(detalle);
    }

    // =========================================================================
    // Métodos del dominio / Patrón Experto (diagramas E1 y E2)
    // =========================================================================

    /**
     * Retorna el número de remito.
     * Corresponde al mensaje getNroRemito() en el diagrama de secuencia.
     */
    public int getNroRemito() {
        return this.numero;
    }

    /**
     * CU 28 – Paso 11b: Actualiza el estado del remito a 'Recibido y Aceptado'
     * y propaga la actualización a sus renglones de documentación.
     */
    public void recibirYAceptar(EstadoRemito nuevoEstado, LocalDateTime fechaHoraActual, Empleado responsable) {
        this.estado = nuevoEstado;
        recibirYAceptarDocumentacion(fechaHoraActual, responsable);
    }

    /**
     * Itera por los detalles del remito delegando la aceptación de la documentación asociada.
     * Corresponde al mensaje recibirYAceptarDocumentacion() en el diagrama de secuencia.
     */
    public void recibirYAceptarDocumentacion(LocalDateTime fechaHoraActual, Empleado responsable) {
        for (DetalleRemito detalle : this.detalleRemito) {
            detalle.recibirYAceptarDocumentacion(fechaHoraActual, responsable);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Remito remito = (Remito) o;
        return numero == remito.numero || (id != null && Objects.equals(id, remito.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return "Remito #" + numero;
    }
}

