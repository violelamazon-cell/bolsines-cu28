package ar.edu.utn.frc.dsi.bolsines.entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "cambios_estado_bolsin")
public class CambioEstadoBolsin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column
    private LocalDateTime fechaHoraFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_bolsin_id", nullable = false)
    private EstadoBolsin estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id", nullable = false)
    private Empleado responsableCE;

    public CambioEstadoBolsin() {
    }

    public CambioEstadoBolsin(LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, EstadoBolsin estado, Empleado responsableCE) {
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.estado = estado;
        this.responsableCE = responsableCE;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public EstadoBolsin getEstado() {
        return estado;
    }

    public void setEstado(EstadoBolsin estado) {
        this.estado = estado;
    }

    public Empleado getResponsableCE() {
        return responsableCE;
    }

    public void setResponsableCE(Empleado responsableCE) {
        this.responsableCE = responsableCE;
    }

    // =========================================================================
    // Métodos del dominio / Patrón Experto (diagramas E1 y E2)
    // =========================================================================

    public boolean esActual() {
        return this.fechaHoraFin == null;
    }

    public boolean esEnviado() {
        return this.estado != null && this.estado.esEnviado();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CambioEstadoBolsin that = (CambioEstadoBolsin) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

