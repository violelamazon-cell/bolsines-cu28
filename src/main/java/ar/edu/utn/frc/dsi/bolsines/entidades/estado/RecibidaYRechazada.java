package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

public class RecibidaYRechazada extends Estado {

    public static final String NOMBRE = "Recibida y rechazada";

    public RecibidaYRechazada() {
        super(NOMBRE, "Documentación rechazada por no corresponder a la CM destino", "Documentacion");
    }
}

