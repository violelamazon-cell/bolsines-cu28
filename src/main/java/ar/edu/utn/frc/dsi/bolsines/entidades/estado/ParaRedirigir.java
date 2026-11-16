package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

import ar.edu.utn.frc.dsi.bolsines.entidades.CambioEstadoDocumentacion;
import ar.edu.utn.frc.dsi.bolsines.entidades.Documentacion;

import java.time.LocalDateTime;
import java.util.List;

public class ParaRedirigir extends Estado {
    public ParaRedirigir() {
    }

    @Override
    public void aceptar(Documentacion doc, LocalDateTime ahora, String[] resp, List<CambioEstadoDocumentacion> cambios) {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }

    @Override
    public void rechazar() {
        throw new UnsupportedOperationException("Transición fuera del alcance de esta entrega");
    }
}
