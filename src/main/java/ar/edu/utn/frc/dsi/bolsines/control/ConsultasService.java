package ar.edu.utn.frc.dsi.bolsines.control;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.BolsinResumenDto;
import ar.edu.utn.frc.dsi.bolsines.boundary.dto.ComisionMedicaDto;
import ar.edu.utn.frc.dsi.bolsines.boundary.excepciones.RecursoNoEncontradoException;
import ar.edu.utn.frc.dsi.bolsines.entidades.Bolsin;
import ar.edu.utn.frc.dsi.bolsines.repositorios.ComisionMedicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ConsultasService – Servicio auxiliar para consultas del frontend:
 * - Listado de comisiones médicas (catálogo para filtros).
 * - Filtro de bolsines enviados en memoria a partir del estado del Gestor.
 */
@Service
public class ConsultasService {

    private final ComisionMedicaRepository comisionMedicaRepository;
    private final GestorRecepcionBolsin gestor;

    public ConsultasService(ComisionMedicaRepository comisionMedicaRepository,
                            GestorRecepcionBolsin gestor) {
        this.comisionMedicaRepository = comisionMedicaRepository;
        this.gestor = gestor;
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

