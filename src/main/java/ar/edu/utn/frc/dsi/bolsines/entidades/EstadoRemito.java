package ar.edu.utn.frc.dsi.bolsines.entidades;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "estados_remito")
public class EstadoRemito {

    public static final String CREADO = "Creado";
    public static final String EN_BOLSIN_SALIENTE = "En bolsín saliente";
    public static final String EN_BOLSIN_ENVIADO = "En bolsín enviado";
    public static final String RECIBIDO_Y_ACEPTADO = "Recibido y Aceptado";
    public static final String RECIBIDO_Y_ACEPTADO_PARCIAL = "Recibido y Aceptado Parcial";
    public static final String CANCELADO = "Cancelado";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    public EstadoRemito() {
    }

    public EstadoRemito(String nombre, String descripcion) {
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

    public boolean esRecibidoYAceptado() {
        return RECIBIDO_Y_ACEPTADO.equalsIgnoreCase(this.nombre);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EstadoRemito that = (EstadoRemito) o;
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

