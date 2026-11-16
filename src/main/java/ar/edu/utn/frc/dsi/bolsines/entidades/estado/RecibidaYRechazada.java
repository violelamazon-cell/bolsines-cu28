package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

public class RecibidaYRechazada extends Estado {
    public RecibidaYRechazada() {
    }

    @Override
    public void revincular() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }
}
