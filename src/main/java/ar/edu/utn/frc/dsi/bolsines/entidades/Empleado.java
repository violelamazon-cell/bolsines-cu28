package ar.edu.utn.frc.dsi.bolsines.entidades;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "empleados")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comision_medica_id", nullable = false)
    private ComisionMedica asignadoA;

    public Empleado() {
    }

    public Empleado(String nombre, String apellido, String email, ComisionMedica asignadoA) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.asignadoA = asignadoA;
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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ComisionMedica getAsignadoA() {
        return asignadoA;
    }

    public void setAsignadoA(ComisionMedica asignadoA) {
        this.asignadoA = asignadoA;
    }

    // =========================================================================
    // Métodos del dominio / Patrón Experto
    // =========================================================================

    /**
     * Retorna la Comisión Médica a la que está asignado el empleado.
     * Corresponde al mensaje getCM() en el diagrama de secuencia.
     */
    public ComisionMedica getCM() {
        return asignadoA;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Empleado empleado = (Empleado) o;
        return Objects.equals(id, empleado.id) || (email != null && Objects.equals(email, empleado.email));
    }

    @Override
    public int hashCode() {
        return Objects.hash(email != null ? email : id);
    }

    @Override
    public String toString() {
        return getNombreCompleto();
    }
}

