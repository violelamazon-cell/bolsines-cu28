package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

public class EnBolsinSaliente extends Estado {
    public EnBolsinSaliente() {
    }

    @Override
    public void quitarDeBolsin() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    @Override
    public void retirarBolsin() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }
}
