package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

public class RecibidaYAceptada extends Estado {

    public static final String NOMBRE = "Recibida y aceptada";

    public RecibidaYAceptada() {
        super(NOMBRE, "Documentación recibida y verificada correctamente en la CM destino", "Documentacion");
    }
}

