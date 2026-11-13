package ar.edu.utn.frc.dsi.bolsines.config;

import ar.edu.utn.frc.dsi.bolsines.entidades.*;
import ar.edu.utn.frc.dsi.bolsines.entidades.estado.*;
import ar.edu.utn.frc.dsi.bolsines.repositorios.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Carga de datos semilla idempotente para pruebas del Caso de Uso 28.
 *
 * Configura:
 * - Comisiones Médicas (CMC, Córdoba, Mendoza, Rosario, Villa María, Paraná).
 * - Empleados encargados de bolsines (Carlos Gómez en Córdoba = usuario por defecto).
 * - Catálogo de estados de Bolsín y Remito.
 * - Tipos de documento.
 * - Bolsines 1001, 1002, 1003 en estado 'Enviado' hacia CM Córdoba.
 * - Bolsín 1004 'Enviado' hacia CM Mendoza (control: no debe listarse para Córdoba).
 * - Bolsín 1005 'Recibido en CM destino' (control: no debe listarse en pendientes).
 * - Historial completo de cambios de estado para Bolsines y Documentaciones.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final ComisionMedicaRepository comisionMedicaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final EstadoBolsinRepository estadoBolsinRepository;
    private final EstadoRemitoRepository estadoRemitoRepository;
    private final DocumentacionRepository documentacionRepository;
    private final BolsinRepository bolsinRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Value("${bolsines.reset-on-start:false}")
    private boolean resetOnStart;

    public DataLoader(ComisionMedicaRepository comisionMedicaRepository,
                      EmpleadoRepository empleadoRepository,
                      TipoDocumentoRepository tipoDocumentoRepository,
                      EstadoBolsinRepository estadoBolsinRepository,
                      EstadoRemitoRepository estadoRemitoRepository,
                      DocumentacionRepository documentacionRepository,
                      BolsinRepository bolsinRepository,
                      org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.comisionMedicaRepository = comisionMedicaRepository;
        this.empleadoRepository = empleadoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.estadoBolsinRepository = estadoBolsinRepository;
        this.estadoRemitoRepository = estadoRemitoRepository;
        this.documentacionRepository = documentacionRepository;
        this.bolsinRepository = bolsinRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // =====================================================================
        // Reinicio solicitado explícitamente vía propiedad bolsines.reset-on-start
        // =====================================================================
        if (resetOnStart) {
            log.info("DataLoader: Reinicio de base de datos solicitado (bolsines.reset-on-start=true). Purgando datos...");
            bolsinRepository.deleteAll();
            documentacionRepository.deleteAll();
            empleadoRepository.deleteAll();
            comisionMedicaRepository.deleteAll();
            tipoDocumentoRepository.deleteAll();
            estadoBolsinRepository.deleteAll();
            estadoRemitoRepository.deleteAll();

            try {
                jdbcTemplate.execute("ALTER TABLE comisiones_medicas ALTER COLUMN id RESTART WITH 1");
                jdbcTemplate.execute("ALTER TABLE empleados ALTER COLUMN id RESTART WITH 1");
                jdbcTemplate.execute("ALTER TABLE tipos_documento ALTER COLUMN id RESTART WITH 1");
                jdbcTemplate.execute("ALTER TABLE estados_bolsin ALTER COLUMN id RESTART WITH 1");
                jdbcTemplate.execute("ALTER TABLE estados_remito ALTER COLUMN id RESTART WITH 1");
                jdbcTemplate.execute("ALTER TABLE bolsines ALTER COLUMN id RESTART WITH 1");
                jdbcTemplate.execute("ALTER TABLE remitos ALTER COLUMN id RESTART WITH 1");
                jdbcTemplate.execute("ALTER TABLE detalles_remito ALTER COLUMN id RESTART WITH 1");
                jdbcTemplate.execute("ALTER TABLE documentaciones ALTER COLUMN id RESTART WITH 1");
                jdbcTemplate.execute("ALTER TABLE cambios_estado_bolsin ALTER COLUMN id RESTART WITH 1");
                jdbcTemplate.execute("ALTER TABLE cambios_estado_documentacion ALTER COLUMN id RESTART WITH 1");
            } catch (Exception ex) {
                log.debug("No se pudieron reiniciar secuencias de identity: {}", ex.getMessage());
            }
        }

        // =====================================================================
        // Guarda de Idempotencia: si ya existen comisiones, no volver a sembrar
        // =====================================================================
        if (comisionMedicaRepository.count() > 0) {
            log.info("DataLoader: La base de datos ya contiene registros. Omitiendo siembra.");
            return;
        }

        log.info("DataLoader: Iniciando carga de datos semilla...");
        LocalDateTime ahora = LocalDateTime.now();

        // 1. Comisiones Médicas
        ComisionMedica cmc = comisionMedicaRepository.save(new ComisionMedica("CMC-01", "CMC Buenos Aires (central)", "Moreno 401, CABA", "cmc@bolsines.gov.ar", "011-4321-0000"));
        ComisionMedica cmCba = comisionMedicaRepository.save(new ComisionMedica("CMJ-CBA", "CM Córdoba", "Rivadavia 767, Córdoba", "cordoba@bolsines.gov.ar", "0351-420-1111"));
        ComisionMedica cmMza = comisionMedicaRepository.save(new ComisionMedica("CMJ-MDZ", "CM Mendoza", "San Martín 1020, Mendoza", "mendoza@bolsines.gov.ar", "0261-423-2222"));
        ComisionMedica cmRos = comisionMedicaRepository.save(new ComisionMedica("CMJ-ROS", "CM Rosario", "Córdoba 1450, Rosario", "rosario@bolsines.gov.ar", "0341-440-3333"));
        ComisionMedica cmVm = comisionMedicaRepository.save(new ComisionMedica("CMJ-VM", "CM Villa María", "Entre Ríos 950, Villa María", "villamaria@bolsines.gov.ar", "0353-452-4444"));
        ComisionMedica cmPar = comisionMedicaRepository.save(new ComisionMedica("CMJ-PAR", "CM Paraná", "Urquiza 820, Paraná", "parana@bolsines.gov.ar", "0343-421-5555"));

        // 2. Empleados (Encargados de Bolsines)
        Empleado empCba = empleadoRepository.save(new Empleado("Carlos", "Gómez", "carlos.gomez@bolsines.gov.ar", cmCba));
        Empleado empMza = empleadoRepository.save(new Empleado("Ana", "Martínez", "ana.martinez@bolsines.gov.ar", cmMza));
        Empleado empCmc = empleadoRepository.save(new Empleado("Roberto", "Rossi", "roberto.rossi@bolsines.gov.ar", cmc));
        Empleado empVm = empleadoRepository.save(new Empleado("Laura", "Torres", "laura.torres@bolsines.gov.ar", cmVm));
        Empleado empRos = empleadoRepository.save(new Empleado("Martín", "Benítez", "martin.benitez@bolsines.gov.ar", cmRos));

        // 3. Tipos de Documento
        TipoDocumento tipoExp = tipoDocumentoRepository.save(new TipoDocumento("Expediente", "Expediente administrativo o pericial"));
        TipoDocumento tipoDic = tipoDocumentoRepository.save(new TipoDocumento("Dictamen", "Dictamen de comisión médica"));
        TipoDocumento tipoEst = tipoDocumentoRepository.save(new TipoDocumento("Estudio médico", "Estudios complementarios, análisis o radiografías"));
        TipoDocumento tipoCar = tipoDocumentoRepository.save(new TipoDocumento("Carta documento", "Notificaciones fehacientes"));

        // 4. Estados de Bolsín (catálogo)
        EstadoBolsin ebCreado = estadoBolsinRepository.save(new EstadoBolsin(EstadoBolsin.CREADO, "Bolsín generado"));
        EstadoBolsin ebCerrado = estadoBolsinRepository.save(new EstadoBolsin(EstadoBolsin.CERRADO, "Bolsín precintado"));
        EstadoBolsin ebEnviado = estadoBolsinRepository.save(new EstadoBolsin(EstadoBolsin.ENVIADO, "Bolsín retirado por correo en tránsito"));
        EstadoBolsin ebRecibido = estadoBolsinRepository.save(new EstadoBolsin(EstadoBolsin.RECIBIDO_EN_CM_DESTINO, "Bolsín recibido físicamente en CM destino"));
        estadoBolsinRepository.save(new EstadoBolsin(EstadoBolsin.DE_BAJA, "Bolsín anulado"));

        // 5. Estados de Remito (catálogo)
        estadoRemitoRepository.save(new EstadoRemito(EstadoRemito.CREADO, "Remito generado"));
        estadoRemitoRepository.save(new EstadoRemito(EstadoRemito.EN_BOLSIN_SALIENTE, "Remito incluido en bolsín saliente"));
        EstadoRemito erEnBolsinEnviado = estadoRemitoRepository.save(new EstadoRemito(EstadoRemito.EN_BOLSIN_ENVIADO, "Remito en tránsito en bolsín enviado"));
        EstadoRemito erRecibidoYAceptado = estadoRemitoRepository.save(new EstadoRemito(EstadoRemito.RECIBIDO_Y_ACEPTADO, "Remito recibido y aceptado"));
        estadoRemitoRepository.save(new EstadoRemito(EstadoRemito.RECIBIDO_Y_ACEPTADO_PARCIAL, "Remito recibido y aceptado parcialmente"));
        estadoRemitoRepository.save(new EstadoRemito(EstadoRemito.CANCELADO, "Remito cancelado"));

        // =====================================================================
        // BOLSÍN 1001 (Caso de Prueba: Flujo Principal CU 28)
        // Origen: CM Mendoza -> Destino: CM Córdoba. Precinto: 50001. Peso: 1500g.
        // Contiene 2 remitos y 3 documentaciones en estado EnBolsinEnviado.
        // =====================================================================
        Bolsin b1001 = new Bolsin(1001, 50001, ahora.minusDays(3), 1500.0, cmMza, cmCba);
        b1001.agregarCambioEstado(new CambioEstadoBolsin(ahora.minusDays(3), ahora.minusDays(2), ebCreado, empMza));
        b1001.agregarCambioEstado(new CambioEstadoBolsin(ahora.minusDays(2), ahora.minusDays(1), ebCerrado, empMza));
        b1001.agregarCambioEstado(new CambioEstadoBolsin(ahora.minusDays(1), null, ebEnviado, empMza)); // VIGENTE

        // Remito 2001
        Remito r2001 = new Remito(2001, ahora.minusDays(2), cmMza, cmCba, erEnBolsinEnviado);
        Documentacion d3001 = crearDocumentacionConHistorial(3001, "Expediente López c/ ART Prevención", ahora.minusDays(5), cmMza, tipoExp, empMza);
        Documentacion d3002 = crearDocumentacionConHistorial(3002, "Dictamen Comisión Médica N° 45/26", ahora.minusDays(4), cmMza, tipoDic, empMza);
        r2001.agregarDetalle(new DetalleRemito(d3001, null));
        r2001.agregarDetalle(new DetalleRemito(d3002, null));
        b1001.agregarRemito(r2001);

        // Remito 2002
        Remito r2002 = new Remito(2002, ahora.minusDays(2), cmMza, cmCba, erEnBolsinEnviado);
        Documentacion d3003 = crearDocumentacionConHistorial(3003, "Estudio Resonancia Magnética Gómez", ahora.minusDays(4), cmMza, tipoEst, empMza);
        r2002.agregarDetalle(new DetalleRemito(d3003, null));
        b1001.agregarRemito(r2002);

        bolsinRepository.save(b1001);

        // =====================================================================
        // BOLSÍN 1002 (Prueba adicional hacia CM Córdoba)
        // Origen: CM Rosario -> Destino: CM Córdoba. Precinto: 50002. Peso: 850g.
        // =====================================================================
        Bolsin b1002 = new Bolsin(1002, 50002, ahora.minusDays(2), 850.0, cmRos, cmCba);
        b1002.agregarCambioEstado(new CambioEstadoBolsin(ahora.minusDays(2), ahora.minusDays(1), ebCreado, empRos));
        b1002.agregarCambioEstado(new CambioEstadoBolsin(ahora.minusDays(1), null, ebEnviado, empRos)); // VIGENTE

        Remito r2003 = new Remito(2003, ahora.minusDays(1), cmRos, cmCba, erEnBolsinEnviado);
        Documentacion d3004 = crearDocumentacionConHistorial(3004, "Carta Documento Intimación Rodríguez", ahora.minusDays(3), cmRos, tipoCar, empRos);
        Documentacion d3005 = crearDocumentacionConHistorial(3005, "Expediente Morales c/ SMG ART", ahora.minusDays(3), cmRos, tipoExp, empRos);
        r2003.agregarDetalle(new DetalleRemito(d3004, null));
        r2003.agregarDetalle(new DetalleRemito(d3005, null));
        b1002.agregarRemito(r2003);

        bolsinRepository.save(b1002);

        // =====================================================================
        // BOLSÍN 1003 (Prueba adicional hacia CM Córdoba)
        // Origen: CM Villa María -> Destino: CM Córdoba. Precinto: 50003. Peso: 2100g.
        // =====================================================================
        Bolsin b1003 = new Bolsin(1003, 50003, ahora.minusDays(2), 2100.0, cmVm, cmCba);
        b1003.agregarCambioEstado(new CambioEstadoBolsin(ahora.minusDays(2), ahora.minusDays(1), ebCreado, empVm));
        b1003.agregarCambioEstado(new CambioEstadoBolsin(ahora.minusDays(1), null, ebEnviado, empVm)); // VIGENTE

        Remito r2004 = new Remito(2004, ahora.minusDays(1), cmVm, cmCba, erEnBolsinEnviado);
        Documentacion d3006 = crearDocumentacionConHistorial(3006, "Expediente Fernández s/ Determinación Incapacidad", ahora.minusDays(3), cmVm, tipoExp, empVm);
        Documentacion d3007 = crearDocumentacionConHistorial(3007, "Estudio Radiográfico Miembro Superior", ahora.minusDays(3), cmVm, tipoEst, empVm);
        r2004.agregarDetalle(new DetalleRemito(d3006, null));
        r2004.agregarDetalle(new DetalleRemito(d3007, null));
        b1003.agregarRemito(r2004);

        Remito r2005 = new Remito(2005, ahora.minusDays(1), cmVm, cmCba, erEnBolsinEnviado);
        Documentacion d3008 = crearDocumentacionConHistorial(3008, "Dictamen Médico Legal 128/26", ahora.minusDays(3), cmVm, tipoDic, empVm);
        Documentacion d3009 = crearDocumentacionConHistorial(3009, "Carta Documento Notificación Alta Médica", ahora.minusDays(3), cmVm, tipoCar, empVm);
        r2005.agregarDetalle(new DetalleRemito(d3008, null));
        r2005.agregarDetalle(new DetalleRemito(d3009, null));
        b1003.agregarRemito(r2005);

        bolsinRepository.save(b1003);

        // =====================================================================
        // BOLSÍN 1004 (Control: Destinado a CM Mendoza, NO debe listarse para Córdoba)
        // =====================================================================
        Bolsin b1004 = new Bolsin(1004, 50004, ahora.minusDays(1), 750.0, cmCba, cmMza);
        b1004.agregarCambioEstado(new CambioEstadoBolsin(ahora.minusDays(1), null, ebEnviado, empCba));
        Remito r2006 = new Remito(2006, ahora.minusDays(1), cmCba, cmMza, erEnBolsinEnviado);
        Documentacion d3010 = crearDocumentacionConHistorial(3010, "Expediente Traslado Mendoza", ahora.minusDays(2), cmCba, tipoExp, empCba);
        r2006.agregarDetalle(new DetalleRemito(d3010, null));
        b1004.agregarRemito(r2006);

        bolsinRepository.save(b1004);

        // =====================================================================
        // BOLSÍN 1005 (Control: Ya recibido en CM Córdoba, NO debe listarse como pendiente)
        // =====================================================================
        Bolsin b1005 = new Bolsin(1005, 50005, ahora.minusDays(5), 920.0, cmPar, cmCba);
        b1005.agregarCambioEstado(new CambioEstadoBolsin(ahora.minusDays(5), ahora.minusDays(4), ebCreado, empCba));
        b1005.agregarCambioEstado(new CambioEstadoBolsin(ahora.minusDays(4), ahora.minusDays(2), ebEnviado, empCba));
        b1005.agregarCambioEstado(new CambioEstadoBolsin(ahora.minusDays(2), null, ebRecibido, empCba)); // VIGENTE RECIBIDO

        Remito r2007 = new Remito(2007, ahora.minusDays(4), cmPar, cmCba, erRecibidoYAceptado);
        Documentacion d3011 = new Documentacion(3011, "Expediente Suárez c/ La Segunda ART", ahora.minusDays(6), cmPar, tipoExp, new RecibidaYAceptada());
        d3011.agregarCambioEstado(new CambioEstadoDocumentacion(ahora.minusDays(6), ahora.minusDays(4), new Registrada(), empCba));
        d3011.agregarCambioEstado(new CambioEstadoDocumentacion(ahora.minusDays(4), ahora.minusDays(2), new EnBolsinEnviado(), empCba));
        d3011.agregarCambioEstado(new CambioEstadoDocumentacion(ahora.minusDays(2), null, new RecibidaYAceptada(), empCba)); // VIGENTE RECIBIDA
        d3011 = documentacionRepository.save(d3011);
        r2007.agregarDetalle(new DetalleRemito(d3011, null));
        b1005.agregarRemito(r2007);

        bolsinRepository.save(b1005);

        log.info("DataLoader: Carga de datos semilla completada con éxito. Bolsines sembrados: 5.");
    }

    /**
     * Construye una Documentación en estado 'EnBolsinEnviado' con su historial completo
     * y coherente (Registrada -> EnRemito -> EnBolsinSaliente -> EnBolsinEnviado [vigente]).
     */
    private Documentacion crearDocumentacionConHistorial(int numero, String asunto, LocalDateTime fechaPase,
                                                        ComisionMedica cmOrigen, TipoDocumento tipo, Empleado resp) {
        Documentacion doc = new Documentacion(numero, asunto, fechaPase, cmOrigen, tipo, new EnBolsinEnviado());
        doc.agregarCambioEstado(new CambioEstadoDocumentacion(fechaPase, fechaPase.plusDays(1), new Registrada(), resp));
        doc.agregarCambioEstado(new CambioEstadoDocumentacion(fechaPase.plusDays(1), fechaPase.plusDays(2), new EnRemito(), resp));
        doc.agregarCambioEstado(new CambioEstadoDocumentacion(fechaPase.plusDays(2), fechaPase.plusDays(3), new EnBolsinSaliente(), resp));
        doc.agregarCambioEstado(new CambioEstadoDocumentacion(fechaPase.plusDays(3), null, new EnBolsinEnviado(), resp)); // VIGENTE
        return documentacionRepository.save(doc);
    }
}
