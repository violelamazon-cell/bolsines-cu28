package ar.edu.utn.frc.dsi.bolsines;

import ar.edu.utn.frc.dsi.bolsines.entidades.*;
import ar.edu.utn.frc.dsi.bolsines.entidades.estado.EnBolsinEnviado;
import ar.edu.utn.frc.dsi.bolsines.repositorios.BolsinRepository;
import ar.edu.utn.frc.dsi.bolsines.repositorios.ComisionMedicaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BolsinesApplicationTests {

	@Autowired
	private BolsinRepository bolsinRepository;

	@Autowired
	private ComisionMedicaRepository comisionMedicaRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Contexto carga correctamente")
	void contextLoads() {
	}

	@Test
	@Transactional
	@DisplayName("Verificaciones con datos reales de la semilla (a, b, c, d)")
	void verificarDatosSemillaCompletos() {
		// (a) 3 bolsines enviados hacia CM Córdoba: 1001, 1002, 1003
		ComisionMedica cmCba = comisionMedicaRepository.findByCodigo("CMJ-CBA")
				.orElseThrow(() -> new AssertionError("CM Córdoba no encontrada"));

		List<Bolsin> bolsinesCba = bolsinRepository.findByDestino(cmCba);
		assertEquals(4, bolsinesCba.size(), "Debería haber 4 bolsines hacia CM Córdoba en la BD (1001, 1002, 1003, 1005)");

		List<Bolsin> enviadosCba = bolsinesCba.stream()
				.filter(Bolsin::sosEnviado)
				.toList();

		assertEquals(3, enviadosCba.size(), "(a) Debe haber exactamente 3 bolsines en estado 'Enviado' hacia Córdoba");
		List<Integer> nrosEnviados = enviadosCba.stream().map(Bolsin::getNumeroBolsin).sorted().toList();
		assertEquals(List.of(1001, 1002, 1003), nrosEnviados, "(a) Los números de bolsín deben ser 1001, 1002 y 1003");

		// (b) 0 bolsines enviados hacia CM Villa María (estado vacío)
		ComisionMedica cmVm = comisionMedicaRepository.findByCodigo("CMJ-VM")
				.orElseThrow(() -> new AssertionError("CM Villa María no encontrada"));

		List<Bolsin> enviadosHaciaVm = bolsinRepository.findByDestino(cmVm).stream()
				.filter(Bolsin::sosEnviado)
				.toList();
		assertEquals(0, enviadosHaciaVm.size(), "(b) CM Villa María debe tener 0 bolsines enviados (demuestra estado vacío)");

		// (c) Bolsín 1005 ya recibido: NO debe figurar como enviado
		Bolsin b1005 = bolsinRepository.findByNumeroBolsin(1005)
				.orElseThrow(() -> new AssertionError("Bolsín 1005 no encontrado"));
		assertFalse(b1005.sosEnviado(), "(c) Bolsín 1005 ya fue recibido; sosEnviado() debe ser false");
		assertEquals("Recibido en CM destino", b1005.buscarEstadoActual().getEstado().getNombre(),
				"(c) El estado actual del bolsín 1005 debe ser 'Recibido en CM destino'");

		// (d) Cada documentación vigente del bolsín 1001 está en 'En bolsín enviado'
		Bolsin b1001 = bolsinRepository.findByNumeroBolsin(1001)
				.orElseThrow(() -> new AssertionError("Bolsín 1001 no encontrado"));
		assertTrue(b1001.sosEnviado(), "Bolsín 1001 debe estar enviado");
		assertEquals(2, b1001.getRemito().size(), "Bolsín 1001 debe contener 2 remitos");

		List<Documentacion> docs1001 = b1001.getRemito().stream()
				.flatMap(r -> r.getDetalleRemito().stream())
				.map(DetalleRemito::getDocumentacion)
				.toList();

		assertEquals(3, docs1001.size(), "Bolsín 1001 debe contener en total 3 documentaciones");
		for (Documentacion doc : docs1001) {
			assertEquals(EnBolsinEnviado.NOMBRE, doc.getEstado().getNombre(),
					"(d) La documentación #" + doc.getNumero() + " debe tener estado actual '" + EnBolsinEnviado.NOMBRE + "'");
			assertTrue(doc.getEstado() instanceof EnBolsinEnviado,
					"(d) La instancia polimórfica debe ser EnBolsinEnviado");

			// Verificar que el historial tiene su cambio vigente con fechaHoraFin = null
			CambioEstadoDocumentacion cambioVigente = doc.getCambioEstado().stream()
					.filter(CambioEstadoDocumentacion::esActual)
					.findFirst()
					.orElseThrow(() -> new AssertionError("No se encontró cambio vigente para doc #" + doc.getNumero()));

			assertEquals(EnBolsinEnviado.NOMBRE, cambioVigente.getEstado().getNombre(),
					"(d) El cambio vigente debe registrar estado '" + EnBolsinEnviado.NOMBRE + "'");
			assertNull(cambioVigente.getFechaHoraFin(), "(d) El cambio vigente debe tener fechaHoraFin null");
		}
	}

	@Test
	@DisplayName("Consulta H2 real: Listado de tablas generadas y consulta de bolsines enviados de Córdoba")
	void ejecutarConsultaSqlReal() {
		// 1. Listar todas las tablas de usuario creadas por Hibernate en el esquema PUBLIC
		List<String> tablas = jdbcTemplate.queryForList(
				"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME",
				String.class
		);

		System.out.println("=== TABLAS CREADAS EN H2 ===");
		for (String tabla : tablas) {
			System.out.println("TABLA: " + tabla);
		}

		// 2. Ejecutar la consulta SQL real contra la base H2 sembrada
		String sql = "SELECT " +
				"    b.numero_bolsin, " +
				"    b.numero_precinto, " +
				"    b.peso, " +
				"    cmo.nombre AS cm_origen, " +
				"    cmd.nombre AS cm_destino, " +
				"    eb.nombre  AS estado_bolsin, " +
				"    ce.fecha_hora_inicio AS fecha_envio " +
				"FROM bolsines b " +
				"JOIN comisiones_medicas cmo ON b.cm_origen_id = cmo.id " +
				"JOIN comisiones_medicas cmd ON b.cm_destino_id = cmd.id " +
				"JOIN cambios_estado_bolsin ce ON ce.bolsin_id = b.id " +
				"JOIN estados_bolsin eb ON ce.estado_bolsin_id = eb.id " +
				"WHERE cmd.codigo = 'CMJ-CBA' " +
				"  AND ce.fecha_hora_fin IS NULL " +
				"  AND eb.nombre = 'Enviado' " +
				"ORDER BY b.numero_bolsin ASC";

		List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql);

		System.out.println("=== SALIDA REAL DE LA CONSULTA SQL EN H2 ===");
		for (Map<String, Object> fila : filas) {
			System.out.println(String.format("Bolsín: %s | Precinto: %s | Peso: %s g | Origen: %s | Destino: %s | Estado: %s | Enviado: %s",
					fila.get("NUMERO_BOLSIN"),
					fila.get("NUMERO_PRECINTO"),
					fila.get("PESO"),
					fila.get("CM_ORIGEN"),
					fila.get("CM_DESTINO"),
					fila.get("ESTADO_BOLSIN"),
					fila.get("FECHA_ENVIO")
			));
		}

		assertEquals(3, filas.size(), "La consulta SQL en H2 debe retornar exactamente los 3 bolsines enviados hacia Córdoba");
	}
}
