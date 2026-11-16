package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

public class EnRemito extends Estado {
    public EnRemito() {
    }

    @Override
    public void asociarBolsin() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    @Override
    public void cancelar() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }
}
