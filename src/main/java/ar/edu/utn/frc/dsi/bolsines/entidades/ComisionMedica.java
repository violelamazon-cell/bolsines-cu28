package ar.edu.utn.frc.dsi.bolsines.entidades;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "comisiones_medicas")
public class ComisionMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 200)
    private String direccion;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String telefono;

    public ComisionMedica() {
    }

    public ComisionMedica(String codigo, String nombre, String direccion, String email, String telefono) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.direccion = direccion;
        this.email = email;
        this.telefono = telefono;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    // =========================================================================
    // Métodos del dominio / Patrón Experto
    // =========================================================================

    public boolean esDeOrigen(String cmOrigen) {
        if (cmOrigen == null || cmOrigen.trim().isEmpty()) {
            return false;
        }
        return this.nombre.equalsIgnoreCase(cmOrigen.trim()) ||
               this.codigo.equalsIgnoreCase(cmOrigen.trim());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComisionMedica that = (ComisionMedica) o;
        return Objects.equals(id, that.id) || (codigo != null && Objects.equals(codigo, that.codigo));
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo != null ? codigo : id);
    }

    @Override
    public String toString() {
        return nombre;
    }
}

