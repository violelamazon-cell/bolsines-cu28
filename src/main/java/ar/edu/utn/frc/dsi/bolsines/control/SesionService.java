package ar.edu.utn.frc.dsi.bolsines.control;

import ar.edu.utn.frc.dsi.bolsines.boundary.dto.SesionDto;
import ar.edu.utn.frc.dsi.bolsines.boundary.excepciones.DatoInvalidoException;
import ar.edu.utn.frc.dsi.bolsines.boundary.excepciones.RecursoNoEncontradoException;
import ar.edu.utn.frc.dsi.bolsines.entidades.Empleado;
import ar.edu.utn.frc.dsi.bolsines.entidades.Sesion;
import ar.edu.utn.frc.dsi.bolsines.entidades.Usuario;
import ar.edu.utn.frc.dsi.bolsines.repositorios.EmpleadoRepository;
import ar.edu.utn.frc.dsi.bolsines.repositorios.SesionRepository;
import ar.edu.utn.frc.dsi.bolsines.repositorios.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SesionService – Servicio de aplicación encargado del inicio y fin de sesión (login/logout).
 *
 * Mantiene la responsabilidad fuera del GestorRecepcionBolsin, cumpliendo con la consigna de cátedra.
 */
@Service
public class SesionService {

    private static final Logger log = LoggerFactory.getLogger(SesionService.class);

    private final UsuarioRepository usuarioRepository;
    private final SesionRepository sesionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final GestorRecepcionBolsin gestor;

    public SesionService(UsuarioRepository usuarioRepository,
                         SesionRepository sesionRepository,
                         EmpleadoRepository empleadoRepository,
                         GestorRecepcionBolsin gestor) {
        this.usuarioRepository = usuarioRepository;
        this.sesionRepository = sesionRepository;
        this.empleadoRepository = empleadoRepository;
        this.gestor = gestor;
    }

    /**
     * Inicia sesión validando credenciales de usuario y registrando una Sesión activa.
     */
    @Transactional
    public SesionDto login(String nombreUsuario, String password) {
        if (nombreUsuario == null || nombreUsuario.isBlank() || password == null || password.isBlank()) {
            throw new DatoInvalidoException("Debe ingresar usuario y contraseña.");
        }

        Usuario usuario = usuarioRepository.findByNombre(nombreUsuario.trim())
                .orElseThrow(() -> new DatoInvalidoException("Usuario o contraseña incorrectos."));

        if (!usuario.getPassword().equals(password.trim())) {
            throw new DatoInvalidoException("Usuario o contraseña incorrectos.");
        }

        LocalDateTime ahora = LocalDateTime.now();

        // Cerrar sesiones activas previas
        List<Sesion> activas = sesionRepository.findByFechaHoraFinIsNull();
        for (Sesion s : activas) {
            s.setFechaHoraFin(ahora);
            sesionRepository.save(s);
        }

        // Crear nueva sesión activa con fechaHoraInicio
        Sesion nuevaSesion = new Sesion(ahora, usuario);
        sesionRepository.save(nuevaSesion);

        // Reiniciar estado del caso de uso en el gestor
        gestor.finCU();

        log.info("Sesión iniciada con éxito para el usuario: {}", usuario.getNombre());
        return construirSesionDto(usuario);
    }

    /**
     * Cierra la sesión activa actual seteando fechaHoraFin.
     */
    @Transactional
    public void logout() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Sesion> activas = sesionRepository.findByFechaHoraFinIsNull();
        for (Sesion s : activas) {
            s.setFechaHoraFin(ahora);
            sesionRepository.save(s);
        }
        gestor.finCU();
        log.info("Sesión cerrada.");
    }

    /**
     * Retorna los datos de la sesión actualmente activa.
     */
    @Transactional(readOnly = true)
    public SesionDto getSesionActiva() {
        Sesion sesionActiva = sesionRepository.findTopByFechaHoraFinIsNullOrderByFechaHoraInicioDesc()
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay una sesión activa de usuario."));

        Usuario usuario = sesionActiva.getUsuarioLogueado();
        if (usuario == null) {
            throw new DatoInvalidoException("La sesión activa no tiene un usuario asociado.");
        }

        return construirSesionDto(usuario);
    }

    private SesionDto construirSesionDto(Usuario usuario) {
        Empleado emp = empleadoRepository.findAll().stream()
                .filter(e -> e.esTuUsuario(usuario))
                .findFirst()
                .orElse(null);

        if (emp != null) {
            return new SesionDto(
                    emp.getId(),
                    emp.getNombre() + " " + emp.getApellido(),
                    emp.getEmail(),
                    emp.getCM().getCodigo(),
                    emp.getCM().getNombre()
            );
        }
        return new SesionDto(null, usuario.getNombre(), null, null, null);
    }
}

