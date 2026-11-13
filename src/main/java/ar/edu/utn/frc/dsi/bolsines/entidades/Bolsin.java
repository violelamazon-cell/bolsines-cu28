package ar.edu.utn.frc.dsi.bolsines.entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "bolsines")
public class Bolsin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int numeroBolsin;

    @Column(nullable = false)
    private int numeroPrecinto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private Double peso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cm_origen_id", nullable = false)
    private ComisionMedica origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cm_destino_id", nullable = false)
    private ComisionMedica destino;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "bolsin_id")
    private List<Remito> remito = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("fechaHoraInicio ASC")
    @JoinColumn(name = "bolsin_id")
    private List<CambioEstadoBolsin> cambioEstado = new ArrayList<>();

    public Bolsin() {
    }

    public Bolsin(int numeroBolsin, int numeroPrecinto, LocalDateTime fecha, Double peso, ComisionMedica origen, ComisionMedica destino) {
        this.numeroBolsin = numeroBolsin;
        this.numeroPrecinto = numeroPrecinto;
        this.fecha = fecha;
        this.peso = peso;
        this.origen = origen;
        this.destino = destino;
    }

    public Long getId() {
        return id;
    }

    public int getNumeroBolsin() {
        return numeroBolsin;
    }

    public void setNumeroBolsin(int numeroBolsin) {
        this.numeroBolsin = numeroBolsin;
    }

    public int getNumeroPrecinto() {
        return numeroPrecinto;
    }

    public void setNumeroPrecinto(int numeroPrecinto) {
        this.numeroPrecinto = numeroPrecinto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Double getPeso() {
        return peso;
    }

    public void setPeso(Double peso) {
        this.peso = peso;
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

    public List<Remito> getRemito() {
        return remito;
    }

    public void setRemito(List<Remito> remito) {
        this.remito = remito;
    }

    public List<CambioEstadoBolsin> getCambioEstado() {
        return cambioEstado;
    }

    public void setCambioEstado(List<CambioEstadoBolsin> cambioEstado) {
        this.cambioEstado = cambioEstado;
    }

    public void agregarRemito(Remito r) {
        this.remito.add(r);
    }

    public void agregarCambioEstado(CambioEstadoBolsin c) {
        this.cambioEstado.add(c);
    }

    // =========================================================================
    // Métodos del dominio / Patrón Experto (diagramas E1 y E2)
    // =========================================================================

    /**
     * CU 28 – Paso 3: Determina si el bolsín se encuentra actualmente en estado 'Enviado'.
     *
     * Aplica el patrón experto: consulta al CambioEstadoBolsin vigente (sin fechaHoraFin)
     * si su estado corresponde a 'Enviado'.
     */
    public boolean sosEnviado() {
        CambioEstadoBolsin actual = buscarEstadoActual();
        return actual != null && actual.esEnviado();
    }

    /**
     * CU 28 – Paso 3: Verifica si la Comisión Médica pasada es la de destino del bolsín.
     */
    public boolean esTuCMDestino(ComisionMedica cm) {
        if (this.destino == null || cm == null) {
            return false;
        }
        return Objects.equals(this.destino.getId(), cm.getId()) ||
               (this.destino.getCodigo() != null && this.destino.getCodigo().equalsIgnoreCase(cm.getCodigo()));
    }

    /**
     * CU 28 – Paso 4: Retorna el nombre de la Comisión Médica de origen.
     */
    public String buscarCMOrigen() {
        return this.origen != null ? this.origen.getNombre() : "";
    }

    /**
     * CU 28 – Paso 4: Retorna el número de precinto.
     */
    public int getNroPrecinto() {
        return this.numeroPrecinto;
    }

    /**
     * CU 28 – Paso 6: Retorna los remitos contenidos en el bolsín.
     */
    public List<Remito> buscarRemitosBolsin() {
        return this.remito;
    }

    /**
     * CU 28 – Paso 11a: Registra la recepción física del bolsín en la CM destino.
     * Cierra el cambio de estado actual y genera uno nuevo con estado 'Recibido en CM destino'.
     */
    public void recibir(LocalDateTime fechaHoraActual, Empleado responsable, EstadoBolsin estadoRecibido) {
        CambioEstadoBolsin actual = buscarEstadoActual();
        if (actual != null) {
            actual.setFechaHoraFin(fechaHoraActual);
        }
        CambioEstadoBolsin nuevoCambio = crearCambioEstadoBolsin(fechaHoraActual, responsable, estadoRecibido);
        this.cambioEstado.add(nuevoCambio);
    }

    /**
     * Instancia un nuevo cambio de estado para el bolsín.
     */
    public CambioEstadoBolsin crearCambioEstadoBolsin(LocalDateTime fechaHoraActual, Empleado responsable, EstadoBolsin estadoRecibido) {
        return new CambioEstadoBolsin(fechaHoraActual, null, estadoRecibido, responsable);
    }

    /**
     * CU 28 – Paso 11b: Propaga la recepción y aceptación a todos los remitos del bolsín.
     */
    public void recibirYAceptarRemitos(EstadoRemito estadoRecibidoYAceptado, LocalDateTime fechaHoraActual, Empleado responsable) {
        for (Remito r : this.remito) {
            r.recibirYAceptar(estadoRecibidoYAceptado, fechaHoraActual, responsable);
        }
    }

    /**
     * Busca en la colección el cambio de estado vigente (fechaHoraFin == null).
     */
    public CambioEstadoBolsin buscarEstadoActual() {
        for (CambioEstadoBolsin c : this.cambioEstado) {
            if (c.esActual()) {
                return c;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bolsin bolsin = (Bolsin) o;
        return numeroBolsin == bolsin.numeroBolsin || (id != null && Objects.equals(id, bolsin.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroBolsin);
    }

    @Override
    public String toString() {
        return "Bolsin #" + numeroBolsin + " (Precinto " + numeroPrecinto + ")";
    }
}

