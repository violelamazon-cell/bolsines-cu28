package ar.edu.utn.frc.dsi.bolsines.control;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.BolsinResumenDto;
import ar.edu.utn.frc.dsi.bolsines.boundary.dto.ComisionMedicaDto;
import ar.edu.utn.frc.dsi.bolsines.boundary.excepciones.DatoInvalidoException;
import ar.edu.utn.frc.dsi.bolsines.boundary.excepciones.RecursoNoEncontradoException;
import ar.edu.utn.frc.dsi.bolsines.entidades.*;
import ar.edu.utn.frc.dsi.bolsines.repositorios.BolsinRepository;
import ar.edu.utn.frc.dsi.bolsines.repositorios.ComisionMedicaRepository;
import ar.edu.utn.frc.dsi.bolsines.repositorios.EmpleadoRepository;
import ar.edu.utn.frc.dsi.bolsines.repositorios.SesionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ConsultasService – Servicio auxiliar para consultas del frontend:
 * - Listado de comisiones médicas (catálogo para filtros).
 * - Filtro de bolsines enviados en memoria a partir del estado del Gestor.
 * - Conteo de bolsines pendientes de solo lectura para la homepage.
 */
@Service
public class ConsultasService {

    private final ComisionMedicaRepository comisionMedicaRepository;
    private final GestorRecepcionBolsin gestor;
    private final BolsinRepository bolsinRepository;
    private final SesionRepository sesionRepository;
    private final EmpleadoRepository empleadoRepository;

    public ConsultasService(ComisionMedicaRepository comisionMedicaRepository,
                            GestorRecepcionBolsin gestor,
                            BolsinRepository bolsinRepository,
                            SesionRepository sesionRepository,
                            EmpleadoRepository empleadoRepository) {
        this.comisionMedicaRepository = comisionMedicaRepository;
        this.gestor = gestor;
        this.bolsinRepository = bolsinRepository;
        this.sesionRepository = sesionRepository;
        this.empleadoRepository = empleadoRepository;
    }

    /**
     * Retorna la cantidad de bolsines pendientes de recepción para la Comisión Médica
     * del usuario con sesión activa. Operación de solo lectura que no altera el estado del Gestor.
     */
    @Transactional(readOnly = true)
    public int contarBolsinesPendientes() {
        Sesion sesionActiva = sesionRepository.findTopByFechaHoraFinIsNullOrderByFechaHoraInicioDesc()
                .orElseThrow(() -> new DatoInvalidoException("No hay una sesión activa de usuario."));

        Usuario usuario = sesionActiva.getUsuarioLogueado();
        if (usuario == null) {
            throw new DatoInvalidoException("La sesión activa no tiene un usuario asociado.");
        }

        Empleado empleado = empleadoRepository.findAll().stream()
                .filter(e -> e.esTuUsuario(usuario))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró un empleado asociado al usuario logueado: " + usuario.getNombre()));

        ComisionMedica cmUsuario = empleado.getCM();
        if (cmUsuario == null) {
            return 0;
        }

        List<Bolsin> candidatos = bolsinRepository.findByDestinoWithCambios(cmUsuario);
        return (int) candidatos.stream()
                .filter(b -> b.sosEnviado() && b.esTuCMDestino(cmUsuario))
                .count();
    }

    /**
     * Retorna todas las Comisiones Médicas ordenadas alfabéticamente por nombre.
     */
    @Transactional(readOnly = true)
    public List<ComisionMedicaDto> listarComisionesMedicas() {
        return comisionMedicaRepository.findAll().stream()
                .map(cm -> new ComisionMedicaDto(cm.getNombre()))
                .sorted(Comparator.comparing(ComisionMedicaDto::nombre))
                .collect(Collectors.toList());
    }

    /**
     * Filtra sobre la colección bolsinesEnviados del estado del Gestor
     * por número de precinto (A1) o Comisión Médica de origen (A2).
     */
    @Transactional(readOnly = true)
    public List<BolsinResumenDto> filtrarBolsines(Integer numeroPrecinto, String cmOrigen) {
        List<Bolsin> bolsines = gestor.getBolsinesEnviados();
        if (bolsines == null || bolsines.isEmpty()) {
            gestor.registrarRecepcionBolsin();
            bolsines = gestor.getBolsinesEnviados();
        }

        List<Bolsin> filtrados = bolsines;

        if (numeroPrecinto != null) {
            filtrados = filtrados.stream()
                    .filter(b -> b.getNroPrecinto() == numeroPrecinto)
                    .collect(Collectors.toList());
            if (filtrados.isEmpty()) {
                throw new RecursoNoEncontradoException(
                        "No se encontró un bolsín con el número de precinto ingresado: " + numeroPrecinto);
            }
        }

        if (cmOrigen != null && !cmOrigen.isBlank()) {
            filtrados = filtrados.stream()
                    .filter(b -> b.buscarCMOrigen().toLowerCase().contains(cmOrigen.toLowerCase()))
                    .collect(Collectors.toList());
            if (filtrados.isEmpty()) {
                throw new RecursoNoEncontradoException(
                        "No se encontró un bolsín con la comisión médica de origen ingresada: " + cmOrigen);
            }
        }

        return filtrados.stream()
                .map(b -> new BolsinResumenDto(
                        b.getNumeroBolsin(),
                        b.buscarCMOrigen(),
                        b.getNroPrecinto()))
                .collect(Collectors.toList());
    }
}

