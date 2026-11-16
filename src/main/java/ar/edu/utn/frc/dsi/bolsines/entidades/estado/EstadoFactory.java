package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Fábrica estática para instanciar subclases concretas de Estado a partir del nombre de la clase
 * o su denominación legible.
 */
public final class EstadoFactory {

    private static final Map<String, Supplier<Estado>> REGISTRO;

    static {
        Map<String, Supplier<Estado>> map = new HashMap<>();

        registrar(map, "Registrada", Registrada::new);
        registrar(map, "EnRemito", EnRemito::new);
        registrar(map, "EnBolsinSaliente", EnBolsinSaliente::new);
        registrar(map, "EnBolsinEnviado", EnBolsinEnviado::new);
        registrar(map, "RecibidaYAceptada", RecibidaYAceptada::new);
        registrar(map, "RecibidaYRechazada", RecibidaYRechazada::new);
        registrar(map, "ParaRedirigir", ParaRedirigir::new);
        registrar(map, "NoRecibida", NoRecibida::new);
        registrar(map, "DeBaja", DeBaja::new);

        // Nombres legibles en español (compatibilidad y tolerancia)
        registrar(map, "En bolsin enviado", EnBolsinEnviado::new);
        registrar(map, "Recibida y aceptada", RecibidaYAceptada::new);
        registrar(map, "En bolsin saliente", EnBolsinSaliente::new);
        registrar(map, "En remito", EnRemito::new);
        registrar(map, "Recibida y rechazada", RecibidaYRechazada::new);
        registrar(map, "Para redirigir", ParaRedirigir::new);
        registrar(map, "No recibida", NoRecibida::new);
        registrar(map, "De baja", DeBaja::new);

        REGISTRO = Collections.unmodifiableMap(map);
    }

    private EstadoFactory() {
    }

    private static void registrar(Map<String, Supplier<Estado>> map, String clave, Supplier<Estado> supplier) {
        map.put(normalizar(clave), supplier);
    }

    /**
     * Devuelve una nueva instancia del estado correspondiente al nombre dado.
     */
    public static Estado porNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del estado de Documentacion no puede ser nulo ni vacío.");
        }
        String claveNormalizada = normalizar(nombre);
        Supplier<Estado> supplier = REGISTRO.get(claveNormalizada);
        if (supplier == null) {
            throw new IllegalArgumentException(
                    "Estado de Documentacion no reconocido: '" + nombre + "'. Estados válidos: " +
                    REGISTRO.keySet()
            );
        }
        return supplier.get();
    }

    /**
     * Normaliza la cadena a minúsculas y sin acentos para tolerar diferencias menores de formato.
     */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String s = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD);
        return s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .replaceAll("\\s+", "");
    }
}
