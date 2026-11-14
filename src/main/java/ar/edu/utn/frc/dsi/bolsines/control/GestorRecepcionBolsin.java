package ar.edu.utn.frc.dsi.bolsines.control;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.*;
import ar.edu.utn.frc.dsi.bolsines.boundary.excepciones.*;
import ar.edu.utn.frc.dsi.bolsines.entidades.*;
import ar.edu.utn.frc.dsi.bolsines.externos.Notificacion;
import ar.edu.utn.frc.dsi.bolsines.externos.NotificadorEmail;
import ar.edu.utn.frc.dsi.bolsines.repositorios.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GestorRecepcionBolsin – Control del CU 28 "Registrar recepción de bolsín".
 *
 * Aplica el patrón GRASP Controlador y Experto.
 *
 * Desviación D8: El Gestor NO guarda estado entre llamadas HTTP (no hay campos
 * de instancia como bolsinSeleccionado ni opcionSeleccionada en el bean @Service).
 * Cada método recibe los identificadores necesarios (numeroBolsin, empleadoId),
 * garantizando la seguridad en concurrencia y adaptándose al modelo REST.
 *
 * Desviación D9: Si la notificación de correo (CU 29) falla, la recepción física
 * y documental ya persistida NO se revierte.
 */
@Service
public class GestorRecepcionBolsin {

    private static final Logger log = LoggerFactory.getLogger(GestorRecepcionBolsin.class);

    private final BolsinRepository bolsinRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EstadoBolsinRepository estadoBolsinRepository;
    private final EstadoRemitoRepository estadoRemitoRepository;
    private final NotificadorEmail notificadorEmail;

    public GestorRecepcionBolsin(BolsinRepository bolsinRepository,
                                  EmpleadoRepository empleadoRepository,
                                  EstadoBolsinRepository estadoBolsinRepository,
                                  EstadoRemitoRepository estadoRemitoRepository,
                                  NotificadorEmail notificadorEmail) {
        this.bolsinRepository = bolsinRepository;
        this.empleadoRepository = empleadoRepository;
        this.estadoBolsinRepository = estadoBolsinRepository;
        this.estadoRemitoRepository = estadoRemitoRepository;
        this.notificadorEmail = notificadorEmail;
    }

    // =========================================================================
    // CU 28 – Paso 1 y 2: Iniciar recepción y obtener usuario logueado
    // =========================================================================

    /**
     * CU 28 Paso 2: Inicia el caso de uso recuperando el empleado logueado y
     * buscando los bolsines en estado 'Enviado' dirigidos a su Comisión Médica.
     */
    @Transactional(readOnly = true)
    public IniciarRecepcionResponse registrarRecepcionBolsin(Long empleadoId) {
        Empleado empleado = getUsuarioLogueado(empleadoId);
        ComisionMedica cmUsuario = empleado.getCM();

        List<BolsinResumenDto> bolsines = buscarBolsinesEnviados(cmUsuario);

        if (bolsines.isEmpty()) {
            throw new RecursoNoEncontradoException(
                "No hay bolsines enviados pendientes de recepción para la comisión médica " + cmUsuario.getNombre()
            );
        }

        return new IniciarRecepcionResponse(
            cmUsuario.getNombre(),
            cmUsuario.getCodigo(),
            bolsines
        );
    }

    /**
     * CU 28 Paso 2: Recupera el empleado logueado (simulado en cabecera o parámetro).
     */
    @Transactional(readOnly = true)
    public Empleado getUsuarioLogueado(Long empleadoId) {
        if (empleadoId != null) {
            Optional<Empleado> empOpt = empleadoRepository.findById(empleadoId);
            if (empOpt.isPresent()) {
                return empOpt.get();
            }
        }
        // Si no se encuentra y se solicita id=1 o null, fallback al empleado de CM Córdoba
        if (empleadoId == null || empleadoId == 1L) {
            return empleadoRepository.findAll().stream()
                .filter(e -> e.getCM() != null && "CMJ-CBA".equalsIgnoreCase(e.getCM().getCodigo()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "No se encontró el empleado con id: " + empleadoId
                ));
        }
        throw new RecursoNoEncontradoException(
            "No se encontró el empleado con id: " + empleadoId
        );
    }

    /**
     * CU 28 Paso 3: Busca los bolsines en estado 'Enviado' cuyo destino es la CM del usuario.
     * Patrón Experto: delega en bolsin.sosEnviado() y bolsin.esTuCMDestino(cmUsuario).
     */
    @Transactional(readOnly = true)
    public List<BolsinResumenDto> buscarBolsinesEnviados(ComisionMedica cmUsuario) {
        List<Bolsin> candidatos = bolsinRepository.findByDestinoWithCambios(cmUsuario);

        return candidatos.stream()
            .filter(b -> b.sosEnviado() && b.esTuCMDestino(cmUsuario))
            .map(b -> new BolsinResumenDto(
                b.getNumeroBolsin(),
                b.buscarCMOrigen(),
                b.getNroPrecinto()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Sobrecarga de conveniencia que resuelve la CM a partir del id de empleado.
     */
    @Transactional(readOnly = true)
    public List<BolsinResumenDto> buscarBolsinesEnviados(Long empleadoId) {
        Empleado empleado = getUsuarioLogueado(empleadoId);
        return buscarBolsinesEnviados(empleado.getCM());
    }

    // =========================================================================
    // CU 28 – Paso 4: Filtros de búsqueda (Flujos Alternativos A1 y A2)
    // =========================================================================

    /**
     * CU 28 Paso 4: Filtra bolsines por número de precinto (A1) o Comisión Médica de origen (A2).
     * Si no se encuentran coincidencias, lanza RecursoNoEncontradoException con mensaje descriptivo.
     */
    @Transactional(readOnly = true)
    public List<BolsinResumenDto> filtrarBolsines(Long empleadoId, Integer numeroPrecinto, String cmOrigen) {
        List<BolsinResumenDto> lista = buscarBolsinesEnviados(empleadoId);

        if (numeroPrecinto != null) {
            lista = lista.stream()
                .filter(b -> b.numeroPrecinto() == numeroPrecinto)
                .collect(Collectors.toList());
            if (lista.isEmpty()) {
                throw new RecursoNoEncontradoException(
                    "No se encontró un bolsín con el número de precinto ingresado: " + numeroPrecinto
                );
            }
        }

        if (cmOrigen != null && !cmOrigen.isBlank()) {
            lista = lista.stream()
                .filter(b -> b.cmOrigen().toLowerCase().contains(cmOrigen.toLowerCase()))
                .collect(Collectors.toList());
            if (lista.isEmpty()) {
                throw new RecursoNoEncontradoException(
                    "No se encontró un bolsín con la comisión médica de origen ingresada: " + cmOrigen
                );
            }
        }

        return lista;
    }

    // =========================================================================
    // CU 28 – Pasos 5 a 7: Tomar selección de bolsín y mostrar remitos/opciones
    // =========================================================================

    /**
     * CU 28 Pasos 5 y 6: Recibe la selección del bolsín, valida que siga en estado 'Enviado'
     * y obtiene el detalle de remitos y opciones de recepción.
     */
    @Transactional(readOnly = true)
    public SeleccionarBolsinResponse tomarSeleccionBolsin(Long empleadoId, int numeroBolsin) {
        Empleado empleado = getUsuarioLogueado(empleadoId);
        ComisionMedica cmUsuario = empleado.getCM();

        Bolsin bolsin = cargarBolsinConRelaciones(numeroBolsin);
        validarBolsinEnviado(bolsin, cmUsuario);

        List<RemitoDto> remitosDto = buscarRemitosBolsin(bolsin);

        List<OpcionRecepcionDto> opciones = List.of(
            new OpcionRecepcionDto(1, "El contenido del bolsín es igual al registrado", true),
            new OpcionRecepcionDto(2, "No se recibe toda la documentación asociada a los remitos que contiene el bolsín", false),
            new OpcionRecepcionDto(3, "Existe documentación que no corresponde al destino", false),
            new OpcionRecepcionDto(4, "La documentación se debe redirigir a otra área", false)
        );

        return new SeleccionarBolsinResponse(
            bolsin.getNumeroBolsin(),
            bolsin.getNroPrecinto(),
            bolsin.getPeso(),
            bolsin.buscarCMOrigen(),
            remitosDto,
            opciones
        );
    }

    /**
     * CU 28 Paso 6: Obtiene los remitos y sus documentaciones (Patrón Experto).
     */
    public List<RemitoDto> buscarRemitosBolsin(Bolsin bolsin) {
        return bolsin.buscarRemitosBolsin().stream()
            .map(r -> {
                List<DocumentacionDto> docs = r.getDetalleRemito().stream()
                    .map(d -> {
                        Documentacion doc = d.buscarDocumentacionRemito();
                        return new DocumentacionDto(
                            doc.getNumero(),
                            doc.buscarTipoDocumentacion(),
                            doc.getAsunto()
                        );
                    })
                    .collect(Collectors.toList());
                return new RemitoDto(r.getNroRemito(), docs);
            })
            .collect(Collectors.toList());
    }

    // =========================================================================
    // CU 28 – Pasos 8 y 9: Tomar selección del tipo de recepción
    // =========================================================================

    /**
     * CU 28 Paso 8: Toma la opción de recepción seleccionada por el Encargado de Bolsines.
     */
    @Transactional(readOnly = true)
    public SeleccionarRecepcionResponse tomarSeleccionRecepcion(Long empleadoId, int numeroBolsin, int opcion) {
        Empleado empleado = getUsuarioLogueado(empleadoId);
        Bolsin bolsin = cargarBolsinConRelaciones(numeroBolsin);
        validarBolsinEnviado(bolsin, empleado.getCM());

        return switch (opcion) {
            case 1 -> {
                int totalDocs = contarDocumentaciones(bolsin);
                yield new SeleccionarRecepcionResponse(
                    1,
                    "El contenido del bolsín es igual al registrado",
                    "Se registrará la recepción de " + totalDocs + " documentación(es) como Recibida y Aceptada.",
                    true
                );
            }
            case 2 -> throw new FuncionalidadNoDisponibleException(
                "La opción 2 (recepción parcial) aún no está disponible. Próximamente."
            );
            case 3 -> throw new FuncionalidadNoDisponibleException(
                "La opción 3 (documentación no corresponde al destino) aún no está disponible. Próximamente."
            );
            case 4 -> throw new FuncionalidadNoDisponibleException(
                "La opción 4 (redirigir documentación) aún no está disponible. Próximamente."
            );
            default -> throw new DatoInvalidoException("La opción debe ser un número entre 1 y 4.");
        };
    }

    // =========================================================================
    // CU 28 – Pasos 10 a 12: Confirmación atómica y notificación CU 29
    // =========================================================================

    /**
     * CU 28 Pasos 10 a 12: Confirma o cancela la recepción del bolsín en una única transacción.
     *
     * Si confirmado == false (Flujo Alternativo A6): no modifica datos y devuelve respuesta informativa.
     * Si confirmado == true:
     *   1. Revalida que el bolsín siga 'Enviado' y pertenezca a la CM destino (evita doble confirmación -> ConflictoEstadoException).
     *   2. Paso 11a: Bolsín -> 'Recibido en CM destino' (cierra cambio actual y abre nuevo).
     *   3. Paso 11b: Remitos -> 'Recibido y Aceptado'.
     *   4. Paso 11c: Documentaciones -> 'Recibida y aceptada' mediante patrón State (GoF).
     *   5. Paso 12: Llama al CU 29 para notificar a cada CM origen distinta.
     */
    @Transactional
    public ConfirmarRecepcionResponse tomarConfirmacion(Long empleadoId, int numeroBolsin, int opcion, boolean confirmado) {
        if (!confirmado) {
            return new ConfirmarRecepcionResponse(
                false,
                "No se registró la recepción. No se modificó ningún dato.",
                null, null, null, null, 0, 0,
                Collections.emptyList()
            );
        }

        if (opcion != 1) {
            throw new FuncionalidadNoDisponibleException(
                "La opción " + opcion + " aún no está disponible. Próximamente."
            );
        }

        Empleado empleado = getUsuarioLogueado(empleadoId);
        ComisionMedica cmUsuario = empleado.getCM();

        Bolsin bolsin = cargarBolsinConRelaciones(numeroBolsin);

        // Patrón Experto: revalidación dentro de la transacción
        if (!bolsin.sosEnviado()) {
            throw new ConflictoEstadoException(
                "El bolsín N° " + numeroBolsin + " ya fue recibido o no se encuentra en estado Enviado."
            );
        }
        if (!bolsin.esTuCMDestino(cmUsuario)) {
            throw new ConflictoEstadoException(
                "El bolsín N° " + numeroBolsin + " no tiene como destino la comisión médica " + cmUsuario.getNombre()
            );
        }

        LocalDateTime fechaHoraActual = getFechaHoraActual();

        // Paso 11a: actualizar estado del bolsín
        EstadoBolsin estadoBolsinRecibido = buscarEstadoBolsinRecibido();
        recibirBolsin(bolsin, fechaHoraActual, empleado, estadoBolsinRecibido);

        // Paso 11b y 11c: actualizar remitos y documentación (patrón State)
        EstadoRemito estadoRemitoRecibido = buscarEstadoRemitoRecibidoYAceptado();
        recibirYAceptarRemitos(bolsin, estadoRemitoRecibido, fechaHoraActual, empleado);

        int totalRemitos = bolsin.buscarRemitosBolsin().size();
        int totalDocs = contarDocumentaciones(bolsin);

        String nuevoEstadoRemitos = estadoRemitoRecibido.getNombre();
        String nuevoEstadoDocumentaciones = bolsin.buscarRemitosBolsin().stream()
            .flatMap(r -> r.getDetalleRemito().stream())
            .map(d -> d.buscarDocumentacionRemito().getEstado().getNombre())
            .findFirst()
            .orElse(null);

        bolsinRepository.save(bolsin);

        // Paso 12: Llamar al CU 29
        List<NotificacionDto> notificaciones = llamarCU29NotificarRecepcionDeBolsin(bolsin, empleado);

        finCU();

        return new ConfirmarRecepcionResponse(
            true,
            "La recepción del bolsín N° " + numeroBolsin + " fue registrada correctamente.",
            numeroBolsin,
            EstadoBolsin.RECIBIDO_EN_CM_DESTINO,
            nuevoEstadoRemitos,
            nuevoEstadoDocumentaciones,
            totalRemitos,
            totalDocs,
            notificaciones
        );
    }

    // =========================================================================
    // Métodos de apoyo del CU 28 según diagramas
    // =========================================================================

    public LocalDateTime getFechaHoraActual() {
        return LocalDateTime.now();
    }

    public EstadoBolsin buscarEstadoBolsinRecibido() {
        return estadoBolsinRepository.findByNombre(EstadoBolsin.RECIBIDO_EN_CM_DESTINO)
            .orElseThrow(() -> new IllegalStateException(
                "Estado de bolsín '" + EstadoBolsin.RECIBIDO_EN_CM_DESTINO + "' no configurado en la base de datos."
            ));
    }

    public EstadoRemito buscarEstadoRemitoRecibidoYAceptado() {
        return estadoRemitoRepository.findByNombre(EstadoRemito.RECIBIDO_Y_ACEPTADO)
            .orElseThrow(() -> new IllegalStateException(
                "Estado de remito '" + EstadoRemito.RECIBIDO_Y_ACEPTADO + "' no configurado en la base de datos."
            ));
    }

    public void recibirBolsin(Bolsin bolsin, LocalDateTime fechaHoraActual, Empleado responsable, EstadoBolsin estadoRecibido) {
        bolsin.recibir(fechaHoraActual, responsable, estadoRecibido);
    }

    public void recibirYAceptarRemitos(Bolsin bolsin, EstadoRemito estadoRemitoRecibido, LocalDateTime fechaHoraActual, Empleado responsable) {
        bolsin.recibirYAceptarRemitos(estadoRemitoRecibido, fechaHoraActual, responsable);
    }

    /**
     * CU 28 Paso 12: Notifica a cada Comisión Médica de origen que aportó documentación.
     * Si ocurre un error de envío (CU 29), se captura y loguea para no revertir la recepción (D9).
     */
    public List<NotificacionDto> llamarCU29NotificarRecepcionDeBolsin(Bolsin bolsin, Empleado responsable) {
        List<NotificacionDto> notificaciones = new ArrayList<>();

        Map<String, ComisionMedica> remitentes = new LinkedHashMap<>();
        for (Remito remito : bolsin.buscarRemitosBolsin()) {
            for (DetalleRemito detalle : remito.getDetalleRemito()) {
                Documentacion doc = detalle.buscarDocumentacionRemito();
                ComisionMedica origen = doc.getCmOrigen();
                if (origen != null) {
                    remitentes.putIfAbsent(origen.getCodigo(), origen);
                }
            }
        }

        for (ComisionMedica origen : remitentes.values()) {
            String asunto = "Recepción del bolsín N° " + bolsin.getNumeroBolsin() + " en " + bolsin.getDestino().getNombre();
            String cuerpo = construirCuerpoNotificacion(bolsin, origen);

            try {
                Notificacion n = notificadorEmail.enviar(origen, asunto, cuerpo);
                notificaciones.add(new NotificacionDto(
                    origen.getNombre(),
                    origen.getEmail(),
                    n.asunto()
                ));
            } catch (Exception e) {
                log.error("CU 29: Error al enviar notificación a {} ({}). La recepción no se revierte.",
                        origen.getNombre(), origen.getEmail(), e);
            }
        }

        return notificaciones;
    }

    public void finCU() {
        log.debug("CU 28 finCU: fin del caso de uso.");
    }

    public void cancelar() {
        finCU();
    }

    // =========================================================================
    // Sesión simulada
    // =========================================================================

    @Transactional(readOnly = true)
    public SesionDto getSesion(Long empleadoId) {
        Empleado emp = getUsuarioLogueado(empleadoId);
        return new SesionDto(
            emp.getId(),
            emp.getNombreCompleto(),
            emp.getCM().getNombre(),
            emp.getCM().getCodigo()
        );
    }

    @Transactional(readOnly = true)
    public List<SesionDto> listarEmpleados() {
        return empleadoRepository.findAll().stream()
            .map(emp -> new SesionDto(
                emp.getId(),
                emp.getNombreCompleto(),
                emp.getCM().getNombre(),
                emp.getCM().getCodigo()
            ))
            .collect(Collectors.toList());
    }

    // =========================================================================
    // Métodos auxiliares privados
    // =========================================================================

    private Bolsin cargarBolsinConRelaciones(int numeroBolsin) {
        Bolsin bolsin = bolsinRepository.findByNumeroBolsin(numeroBolsin)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No se encontró el bolsín N° " + numeroBolsin
            ));

        bolsin.getCambioEstado().size();
        for (Remito r : bolsin.getRemito()) {
            r.getDetalleRemito().size();
            for (DetalleRemito d : r.getDetalleRemito()) {
                d.buscarDocumentacionRemito().getCambioEstado().size();
            }
        }
        return bolsin;
    }

    private void validarBolsinEnviado(Bolsin bolsin, ComisionMedica cmUsuario) {
        if (!bolsin.sosEnviado()) {
            throw new ConflictoEstadoException(
                "El bolsín N° " + bolsin.getNumeroBolsin() + " no está en estado Enviado."
            );
        }
        if (!bolsin.esTuCMDestino(cmUsuario)) {
            throw new RecursoNoEncontradoException(
                "El bolsín N° " + bolsin.getNumeroBolsin() + " no tiene como destino la comisión médica " + cmUsuario.getNombre()
            );
        }
    }

    private int contarDocumentaciones(Bolsin bolsin) {
        return bolsin.buscarRemitosBolsin().stream()
            .mapToInt(r -> r.getDetalleRemito().size())
            .sum();
    }

    private String construirCuerpoNotificacion(Bolsin bolsin, ComisionMedica cmOrigen) {
        StringBuilder sb = new StringBuilder();
        sb.append("Se registró la recepción del bolsín N° ").append(bolsin.getNumeroBolsin());
        sb.append(" en ").append(bolsin.getDestino().getNombre()).append(".\n\n");
        sb.append("Documentación recibida originada en ").append(cmOrigen.getNombre()).append(":\n");

        for (Remito r : bolsin.buscarRemitosBolsin()) {
            for (DetalleRemito d : r.getDetalleRemito()) {
                Documentacion doc = d.buscarDocumentacionRemito();
                if (cmOrigen.getCodigo().equals(doc.getCmOrigen() != null ? doc.getCmOrigen().getCodigo() : null)) {
                    sb.append("  - [").append(doc.buscarTipoDocumentacion()).append("] ");
                    sb.append("N° ").append(doc.getNumero());
                    sb.append(": ").append(doc.getAsunto());
                    sb.append(" -> Recibida y Aceptada\n");
                }
            }
        }
        return sb.toString();
    }
}

