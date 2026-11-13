# Registro de Decisiones de Diseño e Implementación (CU 28)

Este documento registra las decisiones arquitectónicas, tecnológicas y de diseño adoptadas para la implementación del **Caso de Uso 28 "Registrar recepción de bolsín"**, así como las justificaciones de las desviaciones respecto de los diagramas originales de Análisis (E1) y Diseño (E2).

---

## 1. Decisiones Tecnológicas y Arquitectónicas

* **Java 17+ (JDK 25) y Spring Boot 3.5.x:** Se utiliza la última versión estable de la rama 3.5 de Spring Boot (`3.5.16`) con compilación dirigida a Java 17, permitiendo el uso de características modernas como `record` para los DTOs y compatibilidad total con el JDK instalado.
* **Persistencia con H2 en modo archivo (`jdbc:h2:file:./data/bolsines`):** Garantiza que los datos persistan entre reinicios de la aplicación sin requerir la instalación de un servidor de base de datos externo. La consola web `/h2-console` permite visualizar el estado de las tablas durante la defensa oral.
* **Control de transacciones y Open-In-View:** Se desactiva `spring.jpa.open-in-view=false` para asegurar que las transacciones y la carga de relaciones perezosas (`FetchType.LAZY`) se completen dentro de la capa de servicio (`GestorRecepcionBolsin`), evitando `LazyInitializationException` y fugas de conexiones.
* **Sin Lombok:** Todo el código POO se escribe explícitamente con constructores, getters y setters legibles para facilitar la explicación técnica en la defensa.

---

## 2. Decisiones sobre el Patrón State

* **Alcance del Patrón State (consigna de la cátedra):** El patrón State se aplica **únicamente al ciclo de vida de la clase `Documentacion`**. Las clases `Bolsin` y `Remito` gestionan sus estados a través de entidades de catálogo persistidas (`EstadoBolsin` y `EstadoRemito`) y sus respectivos historiales de cambio de estado (`CambioEstadoBolsin`).
* **Persistencia mediante `AttributeConverter` (`EstadoConverter`):** Dado que las clases de estado de `Documentacion` no poseen atributos mutables de instancia ni estado propio, no se modelan como entidades JPA separadas. En su lugar, se persiste su nombre canónico en una columna `VARCHAR` mediante `EstadoConverter` y se reconstituyen a través de `EstadoFactory`. Esto evita redundancia de filas y simplifica la base de datos.
* **Consistencia de `equals()` y `hashCode()` en `Estado`:** Ambas operaciones se basan en el nombre del estado (en minúsculas normalizadas), lo que asegura que Hibernate no detecte modificaciones espurias ("dirty checks") al comparar instancias durante el flush de la transacción.
* **Implementación por defecto en la clase abstracta `Estado`:** Todos los métodos de transición lanzan por defecto `IllegalStateException("Transición no permitida desde el estado: " + getNombre())`. Cada estado concreto sobrescribe únicamente las transiciones válidas para su contexto (en esta entrega, `EnBolsinEnviado.aceptar(...)`).

---

## 3. Desviaciones respecto a los Diagramas (E1 y E2)

| # | Desviación | Justificación |
|---|---|---|
| **D1** | Los métodos de cambio de estado reciben el objeto `Empleado` en lugar de `String[]` (nombre y apellido). | En el diagrama de secuencia E2 figura `empleadoLogueado: string[]`. En el modelo relacional JPA, `CambioEstadoDocumentacion` y `CambioEstadoBolsin` mantienen una asociación `@ManyToOne` con `Empleado responsableCE`. Guardar solo texto degradaría la integridad referencial de la base de datos. |
| **D2** | El `GestorRecepcionBolsin` **no** implementa `buscarEstadoDocumentacionRecibidoYAceptado()` ni almacena el atributo `estadoDocumentacion`. | En el diagrama de clases E2 aún persisten estos elementos por herencia de E1, pero en el diagrama de secuencia E2 ya no se invocan. La responsabilidad de instanciar y asignar el nuevo estado recae exclusivamente en la clase concreta de estado (`EnBolsinEnviado.crearEstado()`), cumpliendo con el patrón State y eliminando acoplamiento en el Gestor. |
| **D3** | Los métodos de salida Gestor → Pantalla (`mostrar...`, `solicitar...`) se sustituyen por retornos de tipo DTO en la API REST. | En una arquitectura Web RESTful, el cliente (frontend) realiza peticiones HTTP y el controlador boundary responde con DTOs JSON. Este criterio sigue el ejemplo oficial provisto por la cátedra ("Administración de Campos"). |
| **D4** | Agrupación de pasos iniciales en llamadas REST compuestas. | Los pasos 1–4 (inicio de recepción y listado de bolsines) y 5–7 (selección de bolsín y detalle de remitos con opciones) se agrupan en endpoints dedicados para evitar llamadas de red redundantes. |
| **D5** | Normalización del nombre del estado `RecibidaYRechazada`. | En el diagrama de clases E2 figuraba `RecibidoYRechazada` (género mixto) y en la máquina de estados `RecibidayRechazada`. Se unificó a `RecibidaYRechazada` en CamelCase respetando el género femenino de `Documentacion`. |
| **D6** | Login y envío de email simulados. | El login se resuelve con un selector de empleado activo en el header de la aplicación, y el envío de notificaciones (CU 29) se realiza mediante un componente `NotificadorEmailSimulado` que registra el envío en logs y devuelve la confirmación, fuera de dependencias con Microsoft Graph o servicios de pago. |
| **D7** | Tratamiento de `EstadoRemito` en la Opción 1. | En el flujo principal implementado (opción 1: contenido igual al registrado), el remito pasa directamente a `Recibido y Aceptado`. El estado `Recibido y Aceptado Parcial` se incluye en el catálogo de datos semilla pero no tiene lógica activa en esta etapa (corresponde a los flujos A3–A5). |

