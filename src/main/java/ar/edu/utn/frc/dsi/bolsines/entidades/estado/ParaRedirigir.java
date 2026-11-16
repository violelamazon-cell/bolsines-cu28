package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

public class ParaRedirigir extends Estado {
    public ParaRedirigir() {
    }

    @Override
    public void rechazar() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }
}
