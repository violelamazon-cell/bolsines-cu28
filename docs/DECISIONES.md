# Registro de Decisiones de Diseño y Desviaciones (CU 28)

**Cátedra:** Diseño de Sistemas de Información (DSI) – UTN Facultad Regional Córdoba (FRC)  
**Año:** 2026 | **Curso:** 3K5 | **Grupo:** 8  
**Caso de Uso:** CU 28 "Registrar recepción de bolsín"

Este documento registra formalmente las decisiones arquitectónicas, tecnológicas y de diseño adoptadas para la implementación del **Caso de Uso 28**, así como las justificaciones de las desviaciones respecto de los diagramas originales de Análisis (E1) y Diseño (E2) exigidas por la consigna de la cátedra.

---

## 1. Decisiones Tecnológicas y de Arquitectura

1. **Java 17 LTS y Spring Boot 3.5.16:** Se utiliza Java 17 como estándar de la industria, aprovechando `record` para DTOs inmutables, `var` local y la API `java.time` (`LocalDateTime`). Spring Boot provee inyección de dependencias, transaccionalidad declarativa (`@Transactional`) y controladores REST.
2. **Persistencia H2 en modo archivo (`jdbc:h2:file:./data/bolsines`):** Garantiza que los cambios de estado persistan entre reinicios de la aplicación sin requerir la instalación ni configuración de un motor de base de datos externo (como PostgreSQL o MySQL). La consola web integrada (`/h2-console`) permite auditar las tablas en vivo durante la defensa.
3. **Desactivación de Open-In-View (`spring.jpa.open-in-view=false`):** Obliga a que todas las consultas y la resolución de asociaciones con carga perezosa (`FetchType.LAZY`) se realicen dentro del límite transaccional de la capa de servicio (`GestorRecepcionBolsin`), previniendo errores de concurrencia y excepciones `LazyInitializationException`.
4. **Ausencia de Lombok:** Se descartó el uso de Project Lombok; todas las entidades cuentan con constructores, getters y setters explícitos en código Java estándar para facilitar la lectura, depuración y explicación oral.
5. **Frontend Estático Liviano y Autónomo:** Single Page Application (SPA) construida en HTML5 semántico, CSS3 y JavaScript puro (ES6+). Se utiliza **Pico.css v2** almacenado localmente en `src/main/resources/static/css/pico.min.css`. **No se utiliza ningún recurso vía CDN, librerías JS externas ni conexión a internet.**

---

## 2. Decisiones sobre el Patrón State (GoF)

1. **Alcance del Patrón State (consigna de la cátedra):** El patrón State se aplica **estrictamente sobre el ciclo de vida de la entidad `Documentacion`**. Las clases `Bolsin` y `Remito` gestionan sus estados a través de entidades de catálogo relacional (`EstadoBolsin` y `EstadoRemito`) y sus respectivos historiales de cambio (`CambioEstadoBolsin`).
2. **Persistencia mediante `AttributeConverter` (`EstadoConverter`):** Dado que los estados de `Documentacion` son clases que representan comportamiento y transiciones sin atributos mutables propios, no se modelan como tablas relacionales independientes. Se almacena su nombre canónico en una columna `VARCHAR(50)` de la tabla `documentaciones` y se reconstituyen mediante `EstadoFactory`. Esto ahorra uniones (`JOIN`) innecesarias y simplifica el esquema de base de datos.
3. **Consistencia en comparaciones (`equals` y `hashCode`):** Ambas operaciones se normalizan por el nombre del estado en minúsculas. Esto garantiza que Hibernate no detecte modificaciones "falsas" (dirty checking) al comparar instancias durante el commit transaccional.
4. **Comportamiento por defecto en clase base `Estado`:** La clase abstracta `Estado` implementa todas las transiciones posibles (`aceptar`, `rechazar`, `redirigir`, `registrarNoRecepcion`) lanzando por defecto `IllegalStateException("Transición no permitida desde el estado: " + getNombre())`. Cada estado concreto sobrescribe únicamente las transiciones válidas para su contexto (en este CU, `EnBolsinEnviado.aceptar(...)`).

---

## 3. Matriz de Desviaciones respecto a los Diagramas (E1 y E2)

| # | Desviación | Justificación Técnica y de Diseño |
|:--:|:---|:---|
| **D1** | **Uso de entidad `Empleado` en lugar de `String[]` (nombre y apellido).** | En el diagrama de secuencia E2 figura el parámetro `empleadoLogueado: string[]`. En el modelo relacional JPA, `CambioEstadoDocumentacion` y `CambioEstadoBolsin` mantienen una asociación `@ManyToOne` con `Empleado responsableCE`. Almacenar solo texto destruiría la integridad referencial y las claves foráneas en la base de datos. |
| **D2** | **El Gestor NO implementa `buscarEstadoDocumentacionRecibidoYAceptado()` ni almacena `estadoDocumentacion`.** | En el diagrama de clases E2 aún persisten estos elementos por arrastre de E1, pero en el diagrama de secuencia E2 ya no se invocan. La responsabilidad de instanciar y asignar el nuevo estado recae exclusivamente en la clase concreta de estado (`EnBolsinEnviado.crearEstado()`), respetando la esencia del **Patrón State** y eliminando acoplamiento indebido en el Gestor. |
| **D3** | **Métodos de salida Gestor → Pantalla (`mostrar...`, `solicitar...`) reemplazados por retornos DTO.** | En una arquitectura Web REST, la pantalla (frontend) realiza peticiones HTTP y el controlador boundary responde sincrónicamente con DTOs en formato JSON. Este criterio replica el ejemplo de referencia provisto por la cátedra ("Administración de Campos"). |
| **D4** | **Agrupación de pasos iniciales en endpoints REST compuestos.** | Los pasos 1–4 (inicio de recepción y listado de bolsines pendientes) y 5–7 (selección de bolsín y detalle de remitos con opciones) se agrupan en endpoints dedicados (`/iniciar` y `/seleccionar-bolsin`) para evitar viajes de red (*roundtrips*) encadenados innecesarios. |
| **D5** | **Persistencia de `Estado` por nombre con `EstadoConverter` y normalización de nomenclatura.** | El estado de `Documentacion` se persiste mediante un conversor JPA por nombre en lugar de una entidad JPA separada (sección 2). Adicionalmente, en el diagrama de clases figuraba `RecibidoYRechazada` (género mixto); se normalizó a `RecibidaYRechazada` en CamelCase concordando con el sustantivo femenino `Documentacion`. |
| **D6** | **Simulación de sesión de usuario y notificación de correo.** | El inicio de sesión se resuelve mediante un selector en el encabezado de la pantalla que envía la cabecera HTTP `X-Empleado-Id` (evitando requerir un servidor de autenticación OAuth2 o LDAP). La notificación a las CM de origen (CU 29) se implementa mediante un componente `NotificadorEmailSimulado` que genera los registros en log sin requerir un servidor SMTP ni servicios externos pagos. |
| **D7** | **Tratamiento del `EstadoRemito` en la Opción 1.** | En el flujo principal implementado (opción 1: contenido igual al registrado), el remito pasa directamente a `Recibido y Aceptado`. El estado `Recibido y Aceptado Parcial` se incluye en el catálogo de datos semilla pero no tiene lógica activa en esta etapa (corresponde a los flujos A3–A5). |
| **D8** | **Endpoints `/seleccionar-recepcion` y `/confirmar` reciben `numeroBolsin` en el body (Gestor Stateless).** | En el diagrama de clases E1 el Gestor guardaba en variables de instancia `bolsinSeleccionado` y `opcionRecepcionSeleccionada`. En una arquitectura Web moderna sobre Spring Boot, el Gestor es un singleton (`@Service`) compartido por todas las peticiones concurrentes. Mantener estado conversacional en campos del Gestor generaría gravísimas condiciones de carrera (*race conditions*) entre distintos usuarios. Por lo tanto, el Gestor se diseñó completamente sin estado (*stateless*): cada petición REST provee el `numeroBolsin` y los parámetros necesarios, resolviéndose atómicamente dentro de su propia transacción. |
| **D9** | **Resiliencia de la transacción ante fallas del notificador externo (CU 29).** | El objetivo principal del CU 28 es registrar la recepción física y documental del bolsín en la base de datos. La notificación por correo es un efecto secundario que pertenece a otro caso de uso (CU 29). Si el servicio de correo fallara (ej. timeout o servidor caído), la transacción de recepción física y actualización de estados ya comprometida **no se revierte**, registrándose una advertencia en el log para auditoría posterior. |
