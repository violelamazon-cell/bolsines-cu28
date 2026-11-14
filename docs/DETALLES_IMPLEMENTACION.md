# Detalles de Implementación Técnica – CU 28 "Registrar recepción de bolsín"

**Cátedra:** Diseño de Sistemas de Información (DSI) – UTN Facultad Regional Córdoba (FRC)  
**Año:** 2026 | **Curso:** 3K5 | **Grupo:** 8  
**Caso de Uso:** CU 28 "Registrar recepción de bolsín"

---

## 1. Ficha Técnica del Proyecto

| Dimensión | Detalle / Especificación |
|:---|:---|
| **Lenguaje de Programación** | **Java 17 LTS** (probado y compatible con JDK 25). Se utilizan características modernas como `record` para DTOs inmutables, `var` local y API `java.time` (`LocalDateTime`). |
| **Framework de Backend** | **Spring Boot 3.5.16** (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-test`). |
| **Arquitectura de Software** | Arquitectura en Capas orientada a Objetos: **Boundary (Presentación / REST)** → **Control (Gestor de CU)** → **Entidades (Modelo de Dominio)** + **Repositorios (Persistencia)**. |
| **Persistencia de Datos** | **JPA 3.1 / Hibernate 6** con base de datos relacional **H2 en modo archivo** (`jdbc:h2:file:./data/bolsines`). Permite persistencia entre reinicios y auditoría en caliente vía `/h2-console`. |
| **Tecnología de Frontend** | **Single Page Application (SPA)** con HTML5 semántico, CSS3 puro con variables y temas sobre **Pico.css v2** (almacenado localmente sin CDN), y **JavaScript Vanilla (ES6+)** asíncrono (`fetch`, `async/await`). |
| **Patrón de Diseño GoF** | **Patrón State** aplicado exhaustivamente sobre el ciclo de vida de la clase de dominio `Documentacion`. |
| **Patrones GRASP** | **Controlador** (`GestorRecepcionBolsin`), **Experto en Información** (`Bolsin`, `Remito`, `DetalleRemito`, `Documentacion`, `Estado`), **Bajo Acoplamiento** y **Alta Cohesión**. |
| **Herramienta de Construcción** | **Apache Maven** con Maven Wrapper (`mvnw`). |

---

## 2. Arquitectura y Mapeo de Capas

El diseño respeta estrictamente la separación de responsabilidades establecida por la cátedra y modelada en los diagramas E1 (Análisis) y E2 (Diseño):

```
┌────────────────────────────────────────────────────────────────────────┐
│                   CAPA DE PRESENTACIÓN / FRONTEND                      │
│   src/main/resources/static/ (index.html, css/app.css, js/app.js)      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTP / JSON
┌───────────────────────────────────▼────────────────────────────────────┐
│                       CAPA BOUNDARY (REST API)                         │
│   ar.edu.utn.frc.dsi.bolsines.boundary                                 │
│   • PantallaRegistrarRecepcionBolsin (@RestController)                 │
│   • SesionController (@RestController)                                │
│   • ManejadorExcepciones (@RestControllerAdvice)                       │
│   • dto.* (Java records: BolsinResumenDto, RemitoDto, etc.)            │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Invocación de Métodos de Servicio
┌───────────────────────────────────▼────────────────────────────────────┐
│                    CAPA DE CONTROL (CASO DE USO)                       │
│   ar.edu.utn.frc.dsi.bolsines.control                                  │
│   • GestorRecepcionBolsin (@Service, @Transactional)                   │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │                                │
┌───────────────────▼──────────────┐   ┌─────────────▼───────────────────┐
│     CAPA DE DOMINIO / ENTIDAD    │   │      SERVICIOS EXTERNOS         │
│   ar.edu.utn.frc.dsi.bolsines.   │   │   ar.edu.utn.frc.dsi.bolsines.  │
│   entidades                      │   │   externos                      │
│   • Bolsin, Remito, DetalleRemito│   │   • NotificadorEmail            │
│   • Documentacion (Contexto)     │   │   • NotificadorEmailSimulado    │
│   • entidades.estado.* (State)   │   │     (Disparo del CU 29)         │
│   • CambioEstadoDocumentacion    │   └─────────────────────────────────┘
│   • CambioEstadoBolsin           │
└───────────────────┬──────────────┘
                    │ JPA / Hibernate
┌───────────────────▼────────────────────────────────────────────────────┐
│                    CAPA DE PERSISTENCIA / REPOSITORIOS                 │
│   ar.edu.utn.frc.dsi.bolsines.repositorios                             │
│   • BolsinRepository, RemitoRepository, DocumentacionRepository...     │
│   • Base de datos H2 en disco (./data/bolsines.mv.db)                  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Implementación del Patrón State (GoF)

### 3.1 Justificación y Problema Resuelto
La consigna de la cátedra exige que el ciclo de vida de la clase `Documentacion` se administre mediante el **Patrón State**, erradicando por completo sentencias condicionales (`if`, `switch`) basadas en strings o enums de estado.

### 3.2 Componentes del Patrón
1. **Contexto (`Documentacion`):**
   * Archivo: `src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/Documentacion.java`
   * Mantiene una referencia polimórfica a la interfaz abstracta `Estado estado`.
   * El método `aceptar(LocalDateTime fechaHoraActual, Empleado responsable)` contiene **cero condicionales**:
     ```java
     public void aceptar(LocalDateTime fechaHoraActual, Empleado responsable) {
         this.estado.aceptar(this, fechaHoraActual, responsable, this.cambioEstado);
     }
     ```
2. **Clase Base Abstracta (`Estado`):**
   * Archivo: `src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/estado/Estado.java`
   * Declara los métodos de transición: `aceptar(...)`, `rechazar(...)`, `redirigir(...)`, `registrarNoRecepcion(...)`.
   * Por defecto, cada método lanza una excepción de negocio `IllegalStateException("Transición no permitida desde el estado: " + getNombre())`. Esto garantiza que los estados concretos solo respondan a los eventos que les son válidos.
3. **Estado Concreto Origen (`EnBolsinEnviado`):**
   * Archivo: `src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/estado/EnBolsinEnviado.java`
   * Sobrescribe el método `aceptar(...)` ejecutando la secuencia exacta definida en el diagrama de secuencia E2 (paso 11c):
     ```java
     @Override
     public void aceptar(Documentacion doc, LocalDateTime ahora, Empleado resp, List<CambioEstadoDocumentacion> cambios) {
         // 1. actual = buscarEstadoActual(cambios)
         CambioEstadoDocumentacion actual = buscarEstadoActual(cambios);
         // 2. actual.setFechaHoraFin(ahora)
         if (actual != null) {
             actual.setFechaHoraFin(ahora);
         }
         // 3. nuevo = crearEstado() [new RecibidaYAceptada()]
         Estado nuevo = crearEstado();
         // 4. cambioNuevo = crearCambioEstado(ahora, resp, nuevo)
         CambioEstadoDocumentacion cambioNuevo = crearCambioEstado(ahora, resp, nuevo);
         // 5. doc.setEstado(nuevo)
         doc.setEstado(nuevo);
         // 6. doc.agregarCambioEstado(cambioNuevo)
         doc.agregarCambioEstado(cambioNuevo);
     }
     ```
4. **Estado Concreto Destino (`RecibidaYAceptada`):**
   * Archivo: `src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/estado/RecibidaYAceptada.java`
   * Representa el estado final exitoso de la documentación recibida conforme.

### 3.3 Persistencia del Patrón State (`EstadoConverter`)
Para no crear tablas redundantes en la base de datos para clases que no tienen atributos mutables de instancia, se utiliza un `AttributeConverter` de JPA:
* Archivo: `src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/estado/EstadoConverter.java`
* En base de datos, la columna `estado` de la tabla `documentaciones` se almacena como `VARCHAR(50)` con el nombre canónico (ej: `"En bolsín enviado"` o `"Recibida y aceptada"`).
* Al leer de la base de datos, `EstadoFactory` reconstituye la instancia concreta del estado correspondiente.

---

## 4. Contrato de la API REST

Todos los endpoints responden en formato JSON y manejan errores de manera uniforme a través de `ManejadorExcepciones` (`@RestControllerAdvice`), devolviendo `{ "mensaje": "..." }`:

| Método | Endpoint | Cabecera / Parámetros | Descripción del Paso | Código HTTP |
|:---|:---|:---|:---|:--:|
| `GET` | `/api/sesion` | `?empleadoId=1` | Obtiene el usuario activo y su Comisión Médica. | `200 OK` |
| `GET` | `/api/sesion/empleados` | — | Lista todos los empleados disponibles para cambiar de usuario. | `200 OK` |
| `POST` | `/api/sesion/iniciar` | Body: `{ "empleadoId": 4 }` | Cambia el usuario activo y reinicia el flujo. | `200 OK` |
| `POST` | `/api/recepcion-bolsin/iniciar` | `X-Empleado-Id: 1` | **Pasos 1–4:** Inicia el caso de uso y lista los bolsines en estado "Enviado" hacia la CM del usuario. | `200 OK` / `404 Not Found` |
| `GET` | `/api/recepcion-bolsin/bolsines` | `?numeroPrecinto=&cmOrigen=` | **Paso 4 (Filtro):** Filtra bolsines por precinto o CM origen. | `200 OK` / `404 Not Found` (A1 / A2) |
| `POST` | `/api/recepcion-bolsin/seleccionar-bolsin` | Body: `{ "numeroBolsin": 1001 }` | **Pasos 5–7:** Selecciona bolsín y devuelve remitos, documentaciones y opciones de recepción. | `200 OK` / `404 Not Found` |
| `POST` | `/api/recepcion-bolsin/seleccionar-recepcion` | Body: `{ "numeroBolsin": 1001, "opcion": 1 }` | **Pasos 8–9:** Valida opción y prepara el resumen de confirmación. Opciones 2, 3 y 4 devuelven 501. | `200 OK` / `501 Not Implemented` |
| `POST` | `/api/recepcion-bolsin/confirmar` | Body: `{ "numeroBolsin": 1001, "opcion": 1, "confirmado": true }` | **Pasos 10–12:** Si es `true`, ejecuta la transacción de recepción física y notificaciones. Retorna `ConfirmarRecepcionResponse` con `nuevoEstadoBolsin`, `nuevoEstadoRemitos`, `nuevoEstadoDocumentaciones`, `remitosActualizados`, `documentacionesActualizadas` y `notificaciones`. Si es `false` (A6), no modifica la base de datos. | `200 OK` / `409 Conflict` |
| `POST` | `/api/recepcion-bolsin/cancelar` | — | **Obs. 1:** Cancela el proceso de recepción en curso sin alterar datos. | `204 No Content` |

---

## 5. Configuración de Base de Datos y Persistencia

En `src/main/resources/application.properties`:
* `spring.datasource.url=jdbc:h2:file:./data/bolsines;DB_CLOSE_ON_EXIT=FALSE`
* `spring.jpa.hibernate.ddl-auto=update`: Mantiene las tablas sincronizadas con las entidades JPA.
* `spring.jpa.open-in-view=false`: Obliga a que toda transacción y carga diferida ocurra dentro del Gestor de aplicación (`@Transactional`), previniendo errores de concurrencia y sesiones abiertas innecesarias.
* `DataLoader.java`: Verifica la existencia de datos (`count() > 0`) antes de sembrar, evitando la duplicación de registros al reiniciar el servidor.

---

## 6. Procedimiento para Correr y Probar

1. **Compilación y arranque:**
   ```powershell
   .\mvnw spring-boot:run
   ```
2. **Acceso al frontend:**
   Navegar con Google Chrome o Mozilla Firefox a `http://localhost:8080`.
3. **Ejecución de tests automatizados:**
   ```powershell
   .\mvnw test
   ```
   * Ejecuta 14 pruebas unitarias de dominio puro (sin Spring ni BD), pruebas de gestor con mocks y pruebas de integración REST de punta a punta sobre H2 en memoria.

