package ar.edu.utn.frc.dsi.bolsines.entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "detalles_remito")
public class DetalleRemito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String areaCMCDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documentacion_id", nullable = false)
    private Documentacion documentacion;

    public DetalleRemito() {
    }

    public DetalleRemito(Documentacion documentacion, String areaCMCDestino) {
        this.documentacion = documentacion;
        this.areaCMCDestino = areaCMCDestino;
    }

    public Long getId() {
        return id;
    }

    public String getAreaCMCDestino() {
        return areaCMCDestino;
    }

    public void setAreaCMCDestino(String areaCMCDestino) {
        this.areaCMCDestino = areaCMCDestino;
    }

    public Documentacion getDocumentacion() {
        return documentacion;
    }

    public void setDocumentacion(Documentacion documentacion) {
        this.documentacion = documentacion;
    }

    // =========================================================================
    // Métodos del dominio / Patrón Experto (diagramas E1 y E2)
    // =========================================================================

    /**
     * Retorna la documentación asociada al renglón del remito.
     * Corresponde al mensaje buscarDocumentacionRemito() en el diagrama de secuencia.
     */
    public Documentacion buscarDocumentacionRemito() {
        return this.documentacion;
    }

    /**
     * Delega la aceptación de la documentación asociada.
     * Corresponde al mensaje recibirYAceptarDocumentacion() en el diagrama de secuencia.
     */
    public void recibirYAceptarDocumentacion(LocalDateTime fechaHoraActual, Empleado responsable) {
        if (this.documentacion != null) {
            this.documentacion.aceptar(fechaHoraActual, responsable);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetalleRemito that = (DetalleRemito) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

