package ar.edu.utn.frc.dsi.bolsines.entidades.estado;

import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Fábrica estática para instanciar subclases concretas de Estado a partir de su nombre.
 *
 * Utilizada por el converter JPA para reconstituir las instancias de Estado
 * persistidas como cadenas en la base de datos.
 */
public final class EstadoFactory {

    private static final Map<String, Supplier<Estado>> REGISTRO;

    static {
        Map<String, Supplier<Estado>> map = new HashMap<>();

        registrar(map, Registrada.NOMBRE, Registrada::new);
        registrar(map, "Registrada", Registrada::new);

        registrar(map, EnRemito.NOMBRE, EnRemito::new);
        registrar(map, "EnRemito", EnRemito::new);

        registrar(map, EnBolsinSaliente.NOMBRE, EnBolsinSaliente::new);
        registrar(map, "EnBolsinSaliente", EnBolsinSaliente::new);

        registrar(map, EnBolsinEnviado.NOMBRE, EnBolsinEnviado::new);
        registrar(map, "EnBolsinEnviado", EnBolsinEnviado::new);

        registrar(map, RecibidaYAceptada.NOMBRE, RecibidaYAceptada::new);
        registrar(map, "RecibidaYAceptada", RecibidaYAceptada::new);

        registrar(map, RecibidaYRechazada.NOMBRE, RecibidaYRechazada::new);
        registrar(map, "RecibidaYRechazada", RecibidaYRechazada::new);

        registrar(map, ParaRedirigir.NOMBRE, ParaRedirigir::new);
        registrar(map, "ParaRedirigir", ParaRedirigir::new);

        registrar(map, NoRecibida.NOMBRE, NoRecibida::new);
        registrar(map, "NoRecibida", NoRecibida::new);

        registrar(map, DeBaja.NOMBRE, DeBaja::new);
        registrar(map, "DeBaja", DeBaja::new);

        REGISTRO = Collections.unmodifiableMap(map);
    }

    private EstadoFactory() {
    }

    private static void registrar(Map<String, Supplier<Estado>> map, String clave, Supplier<Estado> supplier) {
        map.put(normalizar(clave), supplier);
    }

    /**
     * Devuelve una nueva instancia del estado correspondiente al nombre dado.
     *
     * @param nombre Nombre del estado en la base de datos o en el modelo.
     * @return Instancia concreta de Estado.
     * @throws IllegalArgumentException si el nombre es nulo, vacío o no existe (nunca retorna null).
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

