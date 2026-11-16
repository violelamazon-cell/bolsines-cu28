package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertidor JPA para persistir polimórficamente la jerarquía de Estado
 * guardando el nombre simple de la clase (ej. "EnBolsinEnviado", "RecibidaYAceptada")
 * en la base de datos.
 */
@Converter
public class EstadoConverter implements AttributeConverter<Estado, String> {

    @Override
    public String convertToDatabaseColumn(Estado estado) {
        if (estado == null) {
            return null;
        }
        return estado.getClass().getSimpleName();
    }

    @Override
    public Estado convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return EstadoFactory.porNombre(dbData);
    }
}
