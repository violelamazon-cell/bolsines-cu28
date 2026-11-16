package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

public class NoRecibida extends Estado {
    public NoRecibida() {
    }

    @Override
    public void revincular() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    @Override
    public void darDeBaja() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }
}
