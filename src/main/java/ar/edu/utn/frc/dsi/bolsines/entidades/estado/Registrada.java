package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

public class Registrada extends Estado {
    public Registrada() {
    }

    @Override
    public void remitar() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    @Override
    public void darDeBaja() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }
}
