package ar.edu.utn.frc.dsi.bolsines.entidades;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertidor JPA para persistir el responsable (nombre y apellido) como
 * una sola columna String ("nombre|apellido") en la base de datos,
 * respetando el tipo String[] del modelado de análisis y diseño.
 */
@Converter
public class ResponsableConverter implements AttributeConverter<String[], String> {

    @Override
    public String convertToDatabaseColumn(String[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return null;
        }
        String nombre = attribute.length > 0 && attribute[0] != null ? attribute[0] : "";
        String apellido = attribute.length > 1 && attribute[1] != null ? attribute[1] : "";
        return nombre + "|" + apellido;
    }

    @Override
    public String[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        String[] partes = dbData.split("\\|", 2);
        if (partes.length == 1) {
            return new String[]{partes[0], ""};
        }
        return partes;
    }
}

