package ar.edu.utn.frc.dsi.bolsines.entidades;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "tipos_documento")
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    public TipoDocumento() {
    }

    public TipoDocumento(String nombre, String descripcion) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TipoDocumento that = (TipoDocumento) o;
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

