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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    public Empleado() {
    }

    public Empleado(String nombre, String apellido, String email, ComisionMedica asignadoA) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.asignadoA = asignadoA;
    }

    public Empleado(String nombre, String apellido, String email, ComisionMedica asignadoA, Usuario usuario) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.asignadoA = asignadoA;
        this.usuario = usuario;
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

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    /**
     * Evalúa si este empleado está asociado al usuario recibido por parámetro.
     * Corresponde al mensaje esTuUsuario(usuario) en la secuencia E1.
     */
    public boolean esTuUsuario(Usuario usuario) {
        if (this.usuario == null || usuario == null) {
            return false;
        }
        if (this.usuario.getId() != null && usuario.getId() != null) {
            return Objects.equals(this.usuario.getId(), usuario.getId());
        }
        return Objects.equals(this.usuario.getNombre(), usuario.getNombre());
    }

    @Override
    public String toString() {
        return nombre + " " + apellido;
    }
}
