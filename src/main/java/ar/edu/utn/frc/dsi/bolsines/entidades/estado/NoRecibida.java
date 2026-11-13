package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

public class NoRecibida extends Estado {

    public static final String NOMBRE = "No recibida";

    public NoRecibida() {
        super(NOMBRE, "Documentación faltante en el bolsín al momento de la recepción", "Documentacion");
    }
}

