package ar.edu.utn.frc.dsi.bolsines.entidades;

import ar.edu.utn.frc.dsi.bolsines.entidades.estado.Estado;
import ar.edu.utn.frc.dsi.bolsines.entidades.estado.EstadoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "cambios_estado_documentacion")
public class CambioEstadoDocumentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column
    private LocalDateTime fechaHoraFin;

    @Convert(converter = EstadoConverter.class)
    @Column(name = "estado", nullable = false, length = 50)
    private Estado estado;

    @Convert(converter = ResponsableConverter.class)
    @Column(name = "responsable_ce", nullable = false, length = 200)
    private String[] responsableCE;

    public CambioEstadoDocumentacion() {
    }

    public CambioEstadoDocumentacion(LocalDateTime fechaHoraInicio, String[] responsable, Estado estado) {
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = null;
        this.responsableCE = responsable;
        this.estado = estado;
    }

    public CambioEstadoDocumentacion(LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, Estado estado, String[] responsableCE) {
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

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String[] getResponsableCE() {
        return responsableCE;
    }

    public void setResponsableCE(String[] responsableCE) {
        this.responsableCE = responsableCE;
    }

    // =========================================================================
    // Métodos del dominio / Patrón Experto (diagramas E1 y E2)
    // =========================================================================

    public boolean esEstadoActual() {
        return this.fechaHoraFin == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CambioEstadoDocumentacion that = (CambioEstadoDocumentacion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

