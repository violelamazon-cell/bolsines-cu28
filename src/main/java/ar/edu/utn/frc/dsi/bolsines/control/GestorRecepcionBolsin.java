package ar.edu.utn.frc.dsi.bolsines.control;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.*;
import ar.edu.utn.frc.dsi.bolsines.boundary.excepciones.*;
import ar.edu.utn.frc.dsi.bolsines.entidades.*;
import ar.edu.utn.frc.dsi.bolsines.externos.Notificacion;
import ar.edu.utn.frc.dsi.bolsines.externos.NotificadorEmail;
import ar.edu.utn.frc.dsi.bolsines.repositorios.*;
import ar.edu.utn.frc.dsi.bolsines.entidades.ComisionMedica;
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
 * Posee exactamente 5 atributos privados de estado de caso de uso (según
 * modelado de la cátedra):
 * - empleadoLogueado: Empleado
 * - bolsinSeleccionado: Bolsin
 * - bolsinesEnviados: List<Bolsin>
 * - estadoBolsin: EstadoBolsin
 * - estadoRemito: EstadoRemito
 */
@Service
public class GestorRecepcionBolsin {

    private static final Logger log = LoggerFactory.getLogger(GestorRecepcionBolsin.class);

    // Repositorios y componentes de infraestructura
    private final BolsinRepository bolsinRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EstadoBolsinRepository estadoBolsinRepository;
    private final EstadoRemitoRepository estadoRemitoRepository;
    private final ComisionMedicaRepository comisionMedicaRepository;
    private final NotificadorEmail notificadorEmail;

    // Atributos de estado del caso de uso (exactamente 5 según diagrama de clases
    // E2)
    private Empleado empleadoLogueado;
    private Bolsin bolsinSeleccionado;
    private List<Bolsin> bolsinesEnviados;
    private EstadoBolsin estadoBolsin;
    private EstadoRemito estadoRemito;

    public GestorRecepcionBolsin(BolsinRepository bolsinRepository,
            EmpleadoRepository empleadoRepository,
            EstadoBolsinRepository estadoBolsinRepository,
            EstadoRemitoRepository estadoRemitoRepository,
            ComisionMedicaRepository comisionMedicaRepository,
            NotificadorEmail notificadorEmail) {
        this.bolsinRepository = bolsinRepository;
        this.empleadoRepository = empleadoRepository;
        this.estadoBolsinRepository = estadoBolsinRepository;
        this.estadoRemitoRepository = estadoRemitoRepository;
        this.comisionMedicaRepository = comisionMedicaRepository;
        this.notificadorEmail = notificadorEmail;

        // Inicialización de atributos de estado según diagrama
        this.empleadoLogueado = null;
        this.bolsinSeleccionado = null;
        this.bolsinesEnviados = new ArrayList<>();
        this.estadoBolsin = null;
        this.estadoRemito = null;
    }

    public Empleado getEmpleadoLogueado() {
        return empleadoLogueado;
    }

    public Bolsin getBolsinSeleccionado() {
        return bolsinSeleccionado;
    }

    public List<Bolsin> getBolsinesEnviados() {
        return bolsinesEnviados;
    }

    public EstadoBolsin getEstadoBolsin() {
        return estadoBolsin;
    }

    public EstadoRemito getEstadoRemito() {
        return estadoRemito;
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
        this.empleadoLogueado = getUsuarioLogueado(empleadoId);
        this.bolsinSeleccionado = null;
        this.estadoBolsin = null;
        this.estadoRemito = null;

        buscarBolsinesEnviados();

        ComisionMedica cmUsuario = this.empleadoLogueado.getCM();

        if (this.bolsinesEnviados == null || this.bolsinesEnviados.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No hay bolsines enviados pendientes de recepción para la comisión médica "
                            + cmUsuario.getNombre());
        }

        List<BolsinResumenDto> bolsinesDto = this.bolsinesEnviados.stream()
                .map(b -> new BolsinResumenDto(
                        b.getNumeroBolsin(),
                        b.buscarCMOrigen(),
                        b.getNroPrecinto()))
                .collect(Collectors.toList());

        return new IniciarRecepcionResponse(
                cmUsuario.getNombre(),
                cmUsuario.getCodigo(),
                bolsinesDto);
    }

    @Transactional(readOnly = true)
    public IniciarRecepcionResponse registrarRecepcionBolsin() {
        Long empId = this.empleadoLogueado != null ? this.empleadoLogueado.getId() : 1L;
        return registrarRecepcionBolsin(empId);
    }

    /**
     * CU 28 Paso 2: Recupera el empleado logueado (simulado en cabecera o
     * parámetro).
     */
    @Transactional(readOnly = true)
    public Empleado getUsuarioLogueado(Long empleadoId) {
        if (empleadoId != null) {
            Optional<Empleado> empOpt = empleadoRepository.findById(empleadoId);
            if (empOpt.isPresent()) {
                return empOpt.get();
            }
        }
        // Si no se encuentra y se solicita id=1 o null, fallback al empleado de CM
        // Córdoba
        if (empleadoId == null || empleadoId == 1L) {
            return empleadoRepository.findAll().stream()
                    .filter(e -> e.getCM() != null && "CMJ-CBA".equalsIgnoreCase(e.getCM().getCodigo()))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró el empleado con id: " + empleadoId));
        }
        throw new RecursoNoEncontradoException(
                "No se encontró el empleado con id: " + empleadoId);
    }

    /**
     * CU 28 Paso 3: Busca los bolsines en estado 'Enviado' cuyo destino es la CM
     * del usuario
     * y los asigna al atributo de estado bolsinesEnviados.
     */
    @Transactional(readOnly = true)
    public void buscarBolsinesEnviados() {
        if (this.empleadoLogueado == null) {
            return;
        }
        ComisionMedica cmUsuario = this.empleadoLogueado.getCM();
        List<Bolsin> candidatos = bolsinRepository.findByDestinoWithCambios(cmUsuario);
        this.bolsinesEnviados = candidatos.stream()
                .filter(b -> b.sosEnviado() && b.esTuCMDestino(cmUsuario))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // CU 28 – Paso 4: Filtros de búsqueda (Flujos Alternativos A1 y A2)
    // =========================================================================

    /**
     * CU 28 Paso 4: Filtra sobre la colección bolsinesEnviados del Gestor por
     * número de precinto (A1)
     * o Comisión Médica de origen (A2).
     */
    @Transactional(readOnly = true)
    public List<BolsinResumenDto> filtrarBolsines(Integer numeroPrecinto, String cmOrigen) {
        if (this.bolsinesEnviados == null || this.bolsinesEnviados.isEmpty()) {
            registrarRecepcionBolsin(this.empleadoLogueado != null ? this.empleadoLogueado.getId() : 1L);
        }

        List<Bolsin> filtrados = this.bolsinesEnviados;

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

    // =========================================================================
    // CU 28 – Pasos 5 a 7: Tomar selección de bolsín y mostrar remitos/opciones
    // =========================================================================

    /**
     * CU 28 Pasos 5 y 6: Recibe la selección del bolsín, lo asigna al atributo
     * bolsinSeleccionado,
     * valida que siga en estado 'Enviado' y obtiene el detalle de remitos y
     * opciones de recepción.
     */
    @Transactional(readOnly = true)
    public SeleccionarBolsinResponse tomarSeleccionBolsin(int numeroBolsin) {
        if (this.empleadoLogueado == null) {
            this.empleadoLogueado = getUsuarioLogueado(1L);
        }

        Bolsin bolsin = cargarBolsinConRelaciones(numeroBolsin);
        validarBolsinEnviado(bolsin, this.empleadoLogueado.getCM());

        this.bolsinSeleccionado = bolsin;

        List<RemitoDto> remitosDto = buscarRemitosBolsin(this.bolsinSeleccionado);

        List<OpcionRecepcionDto> opciones = List.of(
                new OpcionRecepcionDto(1, "El contenido del bolsín es igual al registrado", true),
                new OpcionRecepcionDto(2,
                        "No se recibe toda la documentación asociada a los remitos que contiene el bolsín", false),
                new OpcionRecepcionDto(3, "Existe documentación que no corresponde al destino", false),
                new OpcionRecepcionDto(4, "La documentación se debe redirigir a otra área", false));

        return new SeleccionarBolsinResponse(
                bolsin.getNumeroBolsin(),
                bolsin.getNroPrecinto(),
                bolsin.getPeso(),
                bolsin.buscarCMOrigen(),
                remitosDto,
                opciones);
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
                                        doc.getAsunto());
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
     * CU 28 Paso 8: Toma la opción de recepción seleccionada por el Encargado de
     * Bolsines.
     */
    @Transactional(readOnly = true)
    public SeleccionarRecepcionResponse tomarSeleccionRecepcion(int opcion) {
        if (this.bolsinSeleccionado == null) {
            throw new ConflictoEstadoException("Debe seleccionar un bolsín antes de elegir una opción de recepción.");
        }

        return switch (opcion) {
            case 1 -> {
                int totalDocs = contarDocumentaciones(this.bolsinSeleccionado);
                yield new SeleccionarRecepcionResponse(
                        1,
                        "El contenido del bolsín es igual al registrado",
                        "Se registrará la recepción de " + totalDocs + " documentación(es) como Recibida y Aceptada.",
                        true);
            }
            case 2 -> throw new FuncionalidadNoDisponibleException(
                    "La opción 2 (recepción parcial) aún no está disponible. Próximamente.");
            case 3 -> throw new FuncionalidadNoDisponibleException(
                    "La opción 3 (documentación no corresponde al destino) aún no está disponible. Próximamente.");
            case 4 -> throw new FuncionalidadNoDisponibleException(
                    "La opción 4 (redirigir documentación) aún no está disponible. Próximamente.");
            default -> throw new DatoInvalidoException("La opción debe ser un número entre 1 y 4.");
        };
    }

    // =========================================================================
    // CU 28 – Pasos 10 a 12: Confirmación atómica y notificación CU 29
    // =========================================================================

    /**
     * CU 28 Pasos 10 a 12: Confirma o cancela la recepción del bolsín trabajando
     * sobre los atributos de estado.
     *
     * Si confirmado == false (Flujo Alternativo A6): no modifica datos, llama a
     * finCU() y devuelve respuesta informativa.
     * Si confirmado == true:
     * 1. Revalida que el bolsín siga 'Enviado' y pertenezca a la CM destino dentro
     * de la transacción.
     * 2. Paso 11a: Setea estadoBolsin y bolsin.recibir(...) -> 'Recibido en CM
     * destino'.
     * 3. Paso 11b: Setea estadoRemito y bolsin.recibirYAceptarRemitos(...) ->
     * 'Recibido y Aceptado'.
     * 4. Paso 11c: Documentaciones -> 'Recibida y aceptada' mediante patrón State
     * (GoF).
     * 5. Paso 12: Llama al CU 29 para notificar a cada CM origen distinta.
     * 6. finCU(): reinicia el estado del gestor.
     */
    @Transactional
    public ConfirmarRecepcionResponse tomarConfirmacion(boolean confirmado) {
        if (this.empleadoLogueado == null || this.bolsinSeleccionado == null) {
            throw new ConflictoEstadoException("Debe seleccionar un bolsín antes de confirmar.");
        }

        if (!confirmado) {
            finCU();
            return new ConfirmarRecepcionResponse(
                    false,
                    "No se registró la recepción. No se modificó ningún dato.",
                    null, null, null, null, 0, 0,
                    Collections.emptyList());
        }

        int nroBolsin = this.bolsinSeleccionado.getNumeroBolsin();

        // Recarga de entidad dentro de la transacción para reasociar con Hibernate
        // Session
        Bolsin bolsin = bolsinRepository.findByNumeroBolsinWithCambios(nroBolsin)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el bolsín N° " + nroBolsin));

        // Inicialización de relaciones para evitar LazyInitializationException
        for (Remito r : bolsin.getRemito()) {
            r.getDetalleRemito().size();
            for (DetalleRemito d : r.getDetalleRemito()) {
                d.buscarDocumentacionRemito().getCambioEstado().size();
            }
        }

        // Patrón Experto: revalidación dentro de la transacción
        if (!bolsin.sosEnviado()) {
            throw new ConflictoEstadoException(
                    "El bolsín N° " + nroBolsin + " ya fue recibido o no se encuentra en estado Enviado.");
        }
        if (!bolsin.esTuCMDestino(this.empleadoLogueado.getCM())) {
            throw new ConflictoEstadoException(
                    "El bolsín N° " + nroBolsin + " no tiene como destino la comisión médica "
                            + this.empleadoLogueado.getCM().getNombre());
        }

        this.bolsinSeleccionado = bolsin;
        buscarEstadoBolsinRecibido();
        buscarEstadoRemitoRecibidoYAceptado();

        LocalDateTime fechaHoraActual = getFechaHoraActual();
        String[] responsable = buscarNombreEmpleado(this.empleadoLogueado);

        // Paso 11a: actualizar estado del bolsín
        recibirBolsin(fechaHoraActual, responsable);

        // Paso 11b y 11c: actualizar remitos y documentación (patrón State)
        recibirYAceptarRemitos(fechaHoraActual, responsable);

        int totalRemitos = this.bolsinSeleccionado.buscarRemitosBolsin().size();
        int totalDocs = contarDocumentaciones(this.bolsinSeleccionado);

        String nuevoEstadoRemitos = this.estadoRemito.getNombre();
        String nuevoEstadoDocumentaciones = "Recibida y aceptada";

        bolsinRepository.save(this.bolsinSeleccionado);

        // Paso 12: Llamar al CU 29
        List<NotificacionDto> notificaciones = llamarCU29NotificarRecepcionDeBolsin(this.bolsinSeleccionado);

        finCU();

        return new ConfirmarRecepcionResponse(
                true,
                "La recepción del bolsín N° " + nroBolsin + " fue registrada correctamente.",
                nroBolsin,
                EstadoBolsin.RECIBIDO_EN_CM_DESTINO,
                nuevoEstadoRemitos,
                nuevoEstadoDocumentaciones,
                totalRemitos,
                totalDocs,
                notificaciones);
    }

    // =========================================================================
    // Métodos de apoyo del CU 28 según diagramas
    // =========================================================================

    public LocalDateTime getFechaHoraActual() {
        return LocalDateTime.now();
    }

    public void buscarEstadoBolsinRecibido() {
        this.estadoBolsin = estadoBolsinRepository.findByNombre(EstadoBolsin.RECIBIDO_EN_CM_DESTINO)
                .orElseThrow(() -> new IllegalStateException(
                        "Estado de bolsín '" + EstadoBolsin.RECIBIDO_EN_CM_DESTINO
                                + "' no configurado en la base de datos."));
    }

    public void buscarEstadoRemitoRecibidoYAceptado() {
        this.estadoRemito = estadoRemitoRepository.findByNombre(EstadoRemito.RECIBIDO_Y_ACEPTADO)
                .orElseThrow(() -> new IllegalStateException(
                        "Estado de remito '" + EstadoRemito.RECIBIDO_Y_ACEPTADO
                                + "' no configurado en la base de datos."));
    }

    public String[] buscarNombreEmpleado(Empleado empleado) {
        if (empleado == null) {
            return new String[] { "", "" };
        }
        return new String[] { empleado.getNombre(), empleado.getApellido() };
    }

    public void recibirBolsin(LocalDateTime fechaHoraActual, String[] responsable) {
        this.bolsinSeleccionado.recibir(fechaHoraActual, responsable, this.estadoBolsin);
    }

    public void recibirYAceptarRemitos(LocalDateTime fechaHoraActual, String[] responsable) {
        this.bolsinSeleccionado.recibirYAceptarRemitos(this.estadoRemito, fechaHoraActual, responsable);
    }

    /**
     * CU 28 Paso 12: Notifica a cada Comisión Médica de origen que aportó
     * documentación.
     */
    public List<NotificacionDto> llamarCU29NotificarRecepcionDeBolsin(Bolsin bolsin) {
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
            String asunto = "Recepción del bolsín N° " + bolsin.getNumeroBolsin() + " en "
                    + bolsin.getDestino().getNombre();
            String cuerpo = construirCuerpoNotificacion(bolsin, origen);

            try {
                Notificacion n = notificadorEmail.enviar(origen, asunto, cuerpo);
                notificaciones.add(new NotificacionDto(
                        origen.getNombre(),
                        origen.getEmail(),
                        n.asunto()));
            } catch (Exception e) {
                log.error(
                        "CU 29: Error al enviar notificación a {} ({}). El error fue capturado; la transacción de recepción puede confirmar al finalizar.",
                        origen.getNombre(), origen.getEmail(), e);
            }
        }

        return notificaciones;
    }

    public void finCU() {
        this.empleadoLogueado = null;
        this.bolsinSeleccionado = null;
        if (this.bolsinesEnviados != null) {
            this.bolsinesEnviados.clear();
        } else {
            this.bolsinesEnviados = new ArrayList<>();
        }
        this.estadoBolsin = null;
        this.estadoRemito = null;
        log.debug("CU 28 finCU: estado del gestor reiniciado.");
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
                emp.getNombre() + " " + emp.getApellido(),
                emp.getCM().getNombre(),
                emp.getCM().getCodigo());
    }

    @Transactional(readOnly = true)
    public List<SesionDto> listarEmpleados() {
        return empleadoRepository.findAll().stream()
                .map(emp -> new SesionDto(
                        emp.getId(),
                        emp.getNombre() + " " + emp.getApellido(),
                        emp.getCM().getNombre(),
                        emp.getCM().getCodigo()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComisionMedicaDto> listarComisionesMedicas() {
        return comisionMedicaRepository.findAll().stream()
                .map(cm -> new ComisionMedicaDto(cm.getNombre()))
                .sorted(Comparator.comparing(ComisionMedicaDto::nombre))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Métodos auxiliares privados
    // =========================================================================

    private Bolsin cargarBolsinConRelaciones(int numeroBolsin) {
        Bolsin bolsin = bolsinRepository.findByNumeroBolsin(numeroBolsin)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el bolsín N° " + numeroBolsin));

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
                    "El bolsín N° " + bolsin.getNumeroBolsin() + " no está en estado Enviado.");
        }
        if (!bolsin.esTuCMDestino(cmUsuario)) {
            throw new RecursoNoEncontradoException(
                    "El bolsín N° " + bolsin.getNumeroBolsin() + " no tiene como destino la comisión médica "
                            + cmUsuario.getNombre());
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
