package ar.edu.utn.frc.dsi.bolsines.boundary.excepciones;

public class DatoInvalidoException extends RuntimeException {
    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}

