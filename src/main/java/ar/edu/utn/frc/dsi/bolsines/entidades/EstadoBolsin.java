package ar.edu.utn.frc.dsi.bolsines.entidades;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "estados_bolsin")
public class EstadoBolsin {

    public static final String CREADO = "Creado";
    public static final String CERRADO = "Cerrado";
    public static final String ENVIADO = "Enviado";
    public static final String RECIBIDO_EN_CM_DESTINO = "Recibido en CM destino";
    public static final String DE_BAJA = "De baja";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    public EstadoBolsin() {
    }

    public EstadoBolsin(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    // =========================================================================
    // Métodos del dominio / Patrón Experto (diagramas E1 y E2)
    // =========================================================================

    public boolean esEnviado() {
        return ENVIADO.equalsIgnoreCase(this.nombre);
    }

    public boolean esRecibidoEnCMDestino() {
        return RECIBIDO_EN_CM_DESTINO.equalsIgnoreCase(this.nombre);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EstadoBolsin that = (EstadoBolsin) o;
        return Objects.equals(id, that.id) || (nombre != null && Objects.equals(nombre, that.nombre));
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre != null ? nombre : id);
    }

    @Override
    public String toString() {
        return nombre;
    }
}

