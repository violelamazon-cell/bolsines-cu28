package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

/**
 * DTO de solo lectura para exponer el nombre de una Comisión Médica.
 * Se usa en GET /api/comisiones-medicas para poblar el desplegable del Paso 1.
 */
public record ComisionMedicaDto(String nombre) {}

