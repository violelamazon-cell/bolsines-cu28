package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertidor JPA para persistir polimórficamente la jerarquía de Estado
 * como una cadena de texto (el nombre canónico del estado) en la base de datos.
 *
 * Evita la creación de tablas adicionales para estados sin atributos mutables.
 */
@Converter
public class EstadoConverter implements AttributeConverter<Estado, String> {

    @Override
    public String convertToDatabaseColumn(Estado estado) {
        if (estado == null) {
            return null;
        }
        return estado.getNombre();
    }

    @Override
    public Estado convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return EstadoFactory.porNombre(dbData);
    }
}

