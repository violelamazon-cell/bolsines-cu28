package ar.edu.utn.frc.dsi.bolsines.boundary.dto;

/**
 * DTO para la respuesta de consulta de solo lectura de bolsines pendientes.
 * Se usa en GET /api/recepcion-bolsin/pendientes.
 */
public record BolsinesPendientesDto(int cantidad) {}

