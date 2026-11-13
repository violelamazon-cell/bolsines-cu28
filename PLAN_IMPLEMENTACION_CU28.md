# Plan de implementación – PPAI Bolsines – CU 28 "Registrar recepción de bolsín"

> Documento para el agente de desarrollo. Leerlo completo antes de escribir código.
> Trabajar **por fases** (sección 11). Al terminar cada fase: compilar, correr tests y reportar qué quedó funcionando.

---

## 1. Contexto

Trabajo práctico de la cátedra **Diseño de Sistemas de Información (UTN FRC, 2026)**. Grupo 8, curso 3K5.
Hay que **implementar el Caso de Uso 28 – Registrar recepción de bolsín** con programación orientada a objetos en **Java**, respetando el modelado entregado:

- **Entrega 1 (Análisis):** diagrama de clases de análisis + diagrama de secuencia + máquina de estados de `Documentacion`.
- **Entrega 2 (Diseño):** rediseño aplicando el **patrón State** (Gamma) **solo para el cambio de estado de `Documentacion`**.
- **Regla de combinación:** todo lo que NO toca el cambio de estado de la Documentación se implementa **como en la Entrega 1**. Lo que toca el cambio de estado de la Documentación se implementa **como en la Entrega 2**.

Material de referencia (el usuario lo copiará a la carpeta `/docs` del proyecto):
`ERS Bolsines`, `Entrega 1 Análisis`, `Entrega 2 Diseño`, y `Administración de Campos` (presentación de la cátedra; es solo **ejemplo de estilo de implementación**, no es parte del dominio).

### Qué evalúa la cátedra (criterios de la consigna)
1. Se puede ejecutar el **flujo principal y al menos dos flujos alternativos**. **El programa debe funcionar el día de la defensa.**
2. **Consistencia con el modelado:** clases gestor, pantalla/boundary, entidades, patrón aplicado.
3. **Persistencia en base de datos** correctamente implementada.
4. El grupo **defiende oralmente** la implementación y muestra el patrón State en el código.
5. Hay que poder describir: lenguaje, framework, tecnología (web/escritorio), base de datos y framework de persistencia.
6. Diseño de UX/UI simple, con patrones de GUI.

> La "implementación contra API real" de la consigna aplica solo al CU 36 (GPS Tracker). **No aplica al CU 28.**

---

## 2. Decisiones tecnológicas (cerradas, no cambiar sin motivo)

Criterio: **gratis, simple de instalar, un solo comando para correr**, sin servicios externos.

| Aspecto | Decisión | Por qué |
|---|---|---|
| Lenguaje | **Java 17** (o 21 si es lo instalado) | Pedido por el alumno; POO |
| Build | **Maven** (con Maven Wrapper `mvnw`) | Estándar, igual que el ejemplo de la cátedra |
| Framework | **Spring Boot 3.x** (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-test`) | Es el que usa el ejemplo de la cátedra; API REST |
| Tecnología | **Web**: backend REST + frontend estático servido por el mismo Spring Boot | Un solo proceso, sin Node ni npm |
| Persistencia | **JPA/Hibernate + H2 en modo archivo** (`jdbc:h2:file:./data/bolsines`) | Gratis, sin instalar nada, los datos sobreviven al reinicio; consola `/h2-console` útil para mostrar persistencia en la defensa |
| Frontend | **HTML + CSS + JavaScript puro** (sin framework), con **Pico.css** descargado dentro de `static/css/` (no por CDN, para que funcione sin internet) | Máxima simplicidad y UI prolija con poco código |
| Lombok | **NO usar** | Código explícito, más fácil de defender |
| DTOs | **`record` de Java 17** | Concisos |
| Email (CU 29) | **Simulado**: interfaz + implementación que loguea y devuelve la notificación a la UI | El ERS pide Outlook/Graph, fuera de alcance y de costo cero |
| Login | **Simulado**: selector "Usuario activo" en el header (ver 8) | `Sesion/Usuario` está en el análisis pero no es el objetivo del CU |

Estilo de implementación a imitar (del material "Administración de Campos"):
- `Pantalla…` = **boundary** = `@RestController` (mantener consistencia con el diagrama).
- `Gestor…` = **control** = `@Service` que conduce el caso de uso.
- Los métodos Gestor→Pantalla del diagrama (`mostrarX()`, `solicitarX()`) **se eliminan**: la información vuelve como **retorno** del método del gestor (consecuencia de una API REST). Documentarlo.
- **Patrón Experto:** el gestor **no** pregunta datos para decidir; pregunta a la entidad (`bolsin.esEnviado()`, no `bolsin.getEstado()` + `if`).
- El boundary **no recibe ni devuelve entidades**: devuelve **DTOs** con datos primitivos.
- Cuando la implementación se aparte del diagrama, dejar una **nota explícita** (ver sección 12).

---

## 3. Estructura del proyecto

```
bolsines-cu28/
├── pom.xml
├── README.md
├── docs/                       # PDFs de referencia + documentación a entregar
│   ├── DETALLES_IMPLEMENTACION.md
│   ├── DECISIONES.md
│   └── GUIA_DEFENSA.md
├── requests.http               # llamadas de prueba a la API (VS Code REST Client / IntelliJ)
└── src/
    ├── main/java/ar/edu/utn/frc/dsi/bolsines/
    │   ├── BolsinesApplication.java
    │   ├── boundary/           # PantallaRegistrarRecepcionBolsin (+ dto/ + manejo de errores)
    │   ├── control/            # GestorRecepcionBolsin
    │   ├── entidades/          # Bolsin, Remito, DetalleRemito, Documentacion, ...
    │   ├── entidades/estado/   # Patrón State: Estado (abstracta) + estados concretos + EstadoConverter
    │   ├── repositorios/       # interfaces Spring Data JPA
    │   ├── externos/           # NotificadorEmail (interfaz CU 29) + implementación simulada
    │   └── config/             # DataLoader (datos semilla)
    ├── main/resources/
    │   ├── application.properties
    │   └── static/             # index.html, css/app.css, css/pico.min.css, js/app.js
    └── test/java/...
```

---

## 4. Modelo de dominio a implementar (solo lo que usa el CU 28)

Omitir `Provincia`, `Localidad` y `Archivo` (no participan del CU). Omitir `Usuario/Sesion` como entidades persistentes (ver 8).

### Entidades JPA y sus relaciones

| Clase | Atributos | Relaciones |
|---|---|---|
| `ComisionMedica` | `codigo`, `nombre`, `direccion`, `email`, `telefono` | — |
| `Empleado` | `nombre`, `apellido`, `email` | `@ManyToOne ComisionMedica asignadoA` |
| `TipoDocumento` | `nombre`, `descripcion` | — |
| `Documentacion` | `numero`(int), `asunto`, `fechaPase` | `@ManyToOne cmOrigen: ComisionMedica`, `@ManyToOne tipoDocumento`, `estado: Estado` (con `@Convert`, ver 5), `@OneToMany(cascade=ALL) cambioEstado: List<CambioEstadoDocumentacion>` |
| `CambioEstadoDocumentacion` | `fechaHoraInicio`, `fechaHoraFin` (nullable) | `estado: Estado` (`@Convert`), `@ManyToOne responsableCE: Empleado` |
| `DetalleRemito` | `areaCMCDestino` (String, nullable) | `@ManyToOne documentacion` |
| `EstadoRemito` | `nombre`, `descripcion` | — (entidad simple, semilla) |
| `Remito` | `numero`(int), `fecha` | `@ManyToOne origen`, `@ManyToOne destino` (ComisionMedica), `@ManyToOne estado: EstadoRemito`, `@OneToMany(cascade=ALL) detalleRemito: List<DetalleRemito>` |
| `EstadoBolsin` | `nombre`, `descripcion` | — (entidad simple, semilla) |
| `CambioEstadoBolsin` | `fechaHoraInicio`, `fechaHoraFin` (nullable) | `@ManyToOne estado: EstadoBolsin`, `@ManyToOne responsableCE: Empleado` |
| `Bolsin` | `numeroBolsin`(int), `numeroPrecinto`(int), `fecha`, `peso`(gramos) | `@ManyToOne origen`, `@ManyToOne destino`, `@OneToMany(cascade=ALL) remito: List<Remito>` (FK en remito), `@OneToMany(cascade=ALL) cambioEstado: List<CambioEstadoBolsin>` |

Reglas de mapeo:
- Cada entidad con `@Id @GeneratedValue Long id` técnico. Los números de negocio (`numeroBolsin`, `numero`) son atributos aparte y únicos donde corresponda.
- `FetchType.LAZY` por defecto y **`spring.jpa.open-in-view=false`**. Toda la lógica y el armado de DTOs ocurre **dentro de un método `@Transactional` del Gestor**, para evitar `LazyInitializationException`.
- Estados de **Bolsín** semilla: `Creado`, `Cerrado`, `Enviado`, `Recibido en CM destino`, `De baja`.
- Estados de **Remito** semilla: `Creado`, `En bolsín saliente`, `En bolsín enviado`, `Recibido y Aceptado`, `Recibido y Aceptado Parcial`, `Cancelado`.

---

## 5. Patrón State para `Documentacion` (núcleo de la defensa)

**Problema que resuelve (para explicar):** gestionar los cambios de estado con `if/switch` produce métodos grandes, rígidos y acoplados; agregar un estado obliga a modificar código existente. State encapsula cada estado en una clase y el contexto delega polimórficamente (cumple OCP y SRP).

### Roles
- **Contexto:** `Documentacion`. Método `aceptar(LocalDateTime fechaHoraActual, Empleado responsable)` **no tiene `if`**: hace `estado.aceptar(this, fechaHoraActual, responsable, cambioEstado)`.
- **Estado (clase abstracta):** `Estado` con `getNombre()`, `getDescripcion()`, `getAmbito()` y los métodos de transición del diagrama: `cancelar, remitar, darDeBaja, asociarBolsin, quitarDeBolsin, revincular, retirarBolsin, registrarNoRecepcion, rechazar, redirigir, aceptar`. **Implementación por defecto de cada transición: lanzar `IllegalStateException("Transición no permitida desde el estado X")`.** Cada estado concreto sobrescribe **solo** las transiciones que le son válidas.
- **Estados concretos** (clases comunes, **sin estado interno**, no son entidades JPA): `Registrada`, `EnRemito`, `EnBolsinSaliente`, `EnBolsinEnviado`, `RecibidaYAceptada`, `RecibidaYRechazada`, `ParaRedirigir`, `NoRecibida`, `DeBaja`.

### Transiciones a implementar en esta entrega
| Estado actual | Método | Estado siguiente | Flujo del CU |
|---|---|---|---|
| `EnBolsinEnviado` | `aceptar(...)` | `RecibidaYAceptada` | **Principal (obligatorio)** |
| `EnBolsinEnviado` | `rechazar(...)` | `RecibidaYRechazada` | A4 (opcional, Fase 5) |
| `EnBolsinEnviado` | `redirigir(...)` | `ParaRedirigir` | A5 (opcional, Fase 5) |
| `EnBolsinEnviado` | `registrarNoRecepcion(...)` | `NoRecibida` | A3 (opcional, Fase 5) |
| `ParaRedirigir` | `aceptar(...)` / `rechazar(...)` | `RecibidaYAceptada` / `RecibidaYRechazada` | A5 (opcional) |

Las demás transiciones de la máquina de estados (cancelar, remitar, etc.) pertenecen a otros CU: **declararlas en `Estado` (default = excepción) y no implementarlas** en los concretos, salvo que sea trivial.

### `EnBolsinEnviado.aceptar(...)` — seguir EXACTAMENTE esta secuencia (diagrama E2)
```
aceptar(Documentacion doc, LocalDateTime ahora, Empleado resp, List<CambioEstadoDocumentacion> cambios):
  1. actual = buscarEstadoActual(cambios)         // itera: cambio.esActual() (fechaHoraFin == null)
  2. actual.setFechaHoraFin(ahora)
  3. nuevo  = crearEstado()                        // new RecibidaYAceptada()
  4. cambioNuevo = crearCambioEstado(ahora, resp, nuevo)   // new CambioEstadoDocumentacion(ahora, resp, nuevo)
  5. doc.setEstado(nuevo)
  6. doc.agregarCambioEstado(cambioNuevo)
```
`buscarEstadoActual`, `crearEstado` y `crearCambioEstado` son métodos **privados** de `EnBolsinEnviado`.

### Persistencia de los estados (decisión de diseño)
Como los estados concretos no tienen datos, **no se guardan como entidades** (evita filas duplicadas por cada `new RecibidaYAceptada()`):
- `EstadoConverter implements AttributeConverter<Estado, String>` guarda el **nombre** del estado y lo reconstruye con una fábrica estática (`Estado.porNombre(String)` con un `Map<String, Supplier<Estado>>`).
- Usar `@Convert(converter = EstadoConverter.class)` en `Documentacion.estado` y `CambioEstadoDocumentacion.estado`.
- Implementar `equals/hashCode` de `Estado` por nombre.

---

## 6. Inventario de clases y métodos (fuente de verdad de nombres)

Usar **estos nombres** (coinciden con los diagramas). Firmas orientativas; el empleado se pasa como `Empleado` (ver desviación D1 en sección 12).

### `PantallaRegistrarRecepcionBolsin` (boundary, `@RestController`, `@RequestMapping("/api/recepcion-bolsin")`)
`opcRegistrarRecepcionBolsin()` · `habilitarPantalla()` · `tomarSeleccionBolsin(...)` · `tomarSeleccionRecepcion(...)` · `tomarConfirmacion(...)` · `cancelar()`.
Cada uno corresponde a un endpoint (sección 7). Se **eliminan** `mostrarNombreCM`, `mostrarBolsinesEnviados`, `mostrarRemitosBolsin`, `solicitarSeleccionRecepcion`, `solicitarConfirmacion` (retornos).

### `GestorRecepcionBolsin` (control, `@Service`, métodos públicos `@Transactional`)
Atributos (estado del CU, uso monousuario de demo): `empleadoLogueado`, `bolsinesEnviados`, `bolsinSeleccionado`, `opcionRecepcionSeleccionada`, `estadoBolsinRecibido`, `estadoRemitoRecibidoYAceptado`.
> Entre requests guardar **ids** y **recargar** las entidades dentro de cada transacción (no retener entidades "detached").

Métodos: `registrarRecepcionBolsin()` · `getUsuarioLogueado()` · `buscarBolsinesEnviados()` · `filtrarBolsines(Integer numeroPrecinto, String cmOrigen)` · `tomarSeleccionBolsin(int numeroBolsin)` · `buscarRemitosBolsin()` · `tomarSeleccionRecepcion(int opcion)` · `tomarConfirmacion(boolean confirmado)` · `getFechaHoraActual()` · `buscarEstadoBolsinRecibido()` · `buscarEstadoRemitoRecibidoYAceptado()` · `buscarNombreEmpleado()` · `recibirBolsin()` · `recibirYAceptarRemitos()` · `llamarCU29NotificarRecepcionDeBolsin()` · `finCU()`.

### Entidades (métodos que delega el gestor — **Patrón Experto**)
- `Empleado`: `getCM()`, `getNombre()`, `getApellido()`, `esTuUsuario(...)` (simplificar si hace falta).
- `ComisionMedica`: `getNombre()`, `esDeOrigen(...)`.
- `Bolsin`: `sosEnviado()`, `esTuCMDestino(ComisionMedica)`, `buscarCMOrigen()`, `getNroPrecinto()`, `getNumeroBolsin()`, `buscarRemitosBolsin()`, `recibir(LocalDateTime, Empleado, EstadoBolsin)`, `crearCambioEstadoBolsin(...)`, `recibirYAceptarRemitos(EstadoRemito, LocalDateTime, Empleado)`.
- `CambioEstadoBolsin`: `esActual()`, `esEnviado()` (delega en `estado.esEnviado()`), `setFechaHoraFin(...)`, `new(...)`.
- `EstadoBolsin`: `esEnviado()`, `esRecibidoEnCMDestino()`.
- `Remito`: `getNroRemito()`, `recibirYAceptar(EstadoRemito, LocalDateTime, Empleado)`, `setEstado(...)`, `recibirYAceptarDocumentacion(LocalDateTime, Empleado)`.
- `EstadoRemito`: `esRecibidoYAceptado()`.
- `DetalleRemito`: `buscarDocumentacionRemito()`, `recibirYAceptarDocumentacion(LocalDateTime, Empleado)` (llama a `documentacion.aceptar(...)`).
- `Documentacion`: `aceptar(LocalDateTime, Empleado)` (**delegación al estado**), `getAsunto()`, `buscarTipoDocumentacion()` (nombre del tipo), `setEstado(Estado)`, `agregarCambioEstado(CambioEstadoDocumentacion)`.
- `CambioEstadoDocumentacion`: `esActual()`, `setFechaHoraFin(...)`, `new(fechaHoraInicio, Empleado, Estado)`.

---

## 7. Contrato de la API REST

Prefijo `/api`. JSON. Errores con cuerpo `{ "mensaje": "texto legible en español" }` vía `@RestControllerAdvice`.
Códigos: `200` ok · `404` no se encontró lo pedido (A1/A2/sin bolsines) · `409` llamada fuera de secuencia o transición de estado inválida · `400` datos inválidos.

### Sesión simulada
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/sesion` | `{empleadoId, nombreCompleto, cm}` del usuario activo |
| GET | `/api/sesion/empleados` | Lista de empleados seleccionables (Encargados de Bolsines) |
| POST | `/api/sesion/iniciar` | Body `{empleadoId}`. Cambia el usuario activo y **reinicia el CU en curso** |

### Caso de uso 28
| Paso del CU | Método y ruta | Body / query | Respuesta 200 |
|---|---|---|---|
| 1–4 | `POST /api/recepcion-bolsin/iniciar` | — | `{cmUsuario, bolsines:[{numeroBolsin, cmOrigen, numeroPrecinto}]}` · **404** si no hay bolsines en estado Enviado para la CM del usuario |
| 4 (filtro) | `GET /api/recepcion-bolsin/bolsines?numeroPrecinto=&cmOrigen=` | query opcionales | misma lista filtrada · **404 (A1)** "No se encontró un bolsín con el número de precinto ingresado" · **404 (A2)** "No se encontró un bolsín con la comisión médica de origen ingresada" |
| 5–7 | `POST /api/recepcion-bolsin/seleccionar-bolsin` | `{numeroBolsin}` | `{numeroBolsin, cmOrigen, numeroPrecinto, remitos:[{numeroRemito, documentaciones:[{numero, tipoDocumento, asunto}]}], opcionesRecepcion:[{codigo, descripcion, habilitada}]}` |
| 8–9 | `POST /api/recepcion-bolsin/seleccionar-recepcion` | `{opcion}` (1–4) | `{opcion, descripcion, resumen, requiereConfirmacion:true}` |
| 10–12 | `POST /api/recepcion-bolsin/confirmar` | `{confirmado:boolean}` | Si `true`: `{confirmado:true, numeroBolsin, estadoBolsin, remitosActualizados, documentacionesActualizadas, notificaciones:[{cmOrigen, email, asunto}]}` · Si `false` (**A6**): `{confirmado:false, mensaje}` sin tocar la BD |
| Obs. 1 | `POST /api/recepcion-bolsin/cancelar` | — | `204` (ejecuta `finCU()`) |

Opciones de recepción (texto textual del CU): 1 "El contenido del bolsín es igual al registrado" · 2 "No se recibe toda la documentación asociada a los remitos que contiene el bolsín" · 3 "Existe documentación que no corresponde al destino" · 4 "La documentación se debe redirigir a otra área". Hasta la Fase 5, solo la 1 viene con `habilitada:true`.

---

## 8. Flujo principal → código (trazabilidad para la defensa)

| Paso CU 28 | Quién | Método |
|---|---|---|
| 1. EB elige registrar recepción | Pantalla | `opcRegistrarRecepcionBolsin()` → `habilitarPantalla()` |
| 2. Muestra CM del usuario | Gestor | `registrarRecepcionBolsin()` → `getUsuarioLogueado()` → `empleado.getCM().getNombre()` |
| 3. Busca bolsines Enviados de la CM | Gestor/Bolsin | `buscarBolsinesEnviados()`: por cada bolsín `sosEnviado()` y `esTuCMDestino(cm)` |
| 4. Muestra CM origen y precinto | Gestor/Bolsin | `buscarCMOrigen()`, `getNroPrecinto()`; filtro `filtrarBolsines(...)` |
| 5. EB elige bolsín | Pantalla→Gestor | `tomarSeleccionBolsin()` |
| 6. Muestra remitos y tipo de documentación | Gestor | `buscarRemitosBolsin()` → `remito.getNroRemito()`, `detalle.buscarDocumentacionRemito()`, `doc.buscarTipoDocumentacion()` |
| 7. Muestra opciones | Gestor | (retorno en el mismo DTO) |
| 8. EB elige opción 1 | Pantalla→Gestor | `tomarSeleccionRecepcion()` |
| 9–10. Pide y toma confirmación | Pantalla→Gestor | `tomarConfirmacion(true)` |
| 11a. Bolsín → "Recibido en CM destino" | Gestor/Bolsin | `getFechaHoraActual()`, `buscarEstadoBolsinRecibido()`, `buscarNombreEmpleado()`, `recibirBolsin()` → `bolsin.recibir(...)` (cierra el cambio de estado actual y crea uno nuevo) |
| 11b. Remitos → "Recibido y Aceptado" | Gestor/Bolsin/Remito | `buscarEstadoRemitoRecibidoYAceptado()`, `recibirYAceptarRemitos()` → `bolsin.recibirYAceptarRemitos(...)` → `remito.recibirYAceptar(...)` |
| 11c. Documentación → "Recibida y Aceptada" | Remito/DetalleRemito/Documentacion/**State** | `remito.recibirYAceptarDocumentacion(...)` → `detalle.recibirYAceptarDocumentacion(...)` → `doc.aceptar(...)` → `EnBolsinEnviado.aceptar(...)` (**patrón State**) |
| 12. Notifica a cada CM origen (CU 29) | Gestor | `llamarCU29NotificarRecepcionDeBolsin()` → `NotificadorEmail` |
| Fin | Gestor | `finCU()` (limpia el estado del gestor) |

- **Todo el paso 11 + 12 en una sola transacción** (`@Transactional`): si algo falla, rollback completo (no puede quedar el bolsín recibido y la documentación sin actualizar).
- El gestor **no** busca "el estado de documentación recibida y aceptada": lo decide el propio estado (ese es el punto del patrón). No implementar `buscarEstadoDocumentacionRecibidoYAceptado` (ver D2).

### Notificación CU 29 (simulada)
Interfaz `NotificadorEmail { Notificacion enviar(ComisionMedica destinataria, String asunto, String cuerpo); }` + `NotificadorEmailSimulado` que escribe en el log y devuelve la notificación.
Se notifica **una vez por cada CM de origen que envió documentación en ese bolsín** (agrupar por `documentacion.cmOrigen`). Cuerpo: número de bolsín, CM destino, y por cada documentación: número, tipo y situación ("Recibida y Aceptada").

---

## 9. Frontend (UX/UI simple y amigable)

Una sola página (`index.html`) con **asistente de 4 pasos (wizard)**. Patrones de GUI a aplicar y nombrar en la defensa: **Wizard / indicador de progreso (stepper)**, **diálogo de confirmación**, **feedback inmediato** (mensajes de éxito/error), **estados vacíos** con mensaje claro, **acción de cancelar siempre visible**, **filtros de búsqueda en tabla**.

**Layout general**
- Header: nombre del sistema "Bolsines", CM del usuario activo y selector **"Usuario activo"** (simula login).
- Stepper: `1 Bolsín` → `2 Revisión` → `3 Confirmación` → `4 Resultado`.
- Contenedor centrado, tarjetas con espacios generosos, botones grandes, un único botón primario por pantalla.

**Paso 1 – Elegir bolsín**
- Título "Bolsines recibidos pendientes de registro" y subtítulo con la CM del usuario.
- Dos campos de filtro (Nº de precinto, CM de origen) + botones *Buscar* y *Limpiar*.
- Tabla: CM origen · Nº de precinto · botón *Seleccionar* (la fila entera también es clickeable). 
- Estados: cargando, vacío ("No hay bolsines enviados para tu comisión médica"), error de búsqueda (A1/A2) en banner claro sin perder la tabla.

**Paso 2 – Revisión y tipo de recepción**
- Resumen del bolsín (origen, precinto).
- Remitos como secciones desplegables (`<details>`); dentro, lista de documentación con **tipo** y asunto.
- Pregunta: "¿Qué recibiste?" con 4 **radio buttons** grandes (la 1 preseleccionada solo si es la única habilitada; las no implementadas se ven deshabilitadas con la leyenda "Próximamente").
- Botones: *Volver* · *Cancelar* · **Continuar**.

**Paso 3 – Confirmación**
- `<dialog>` modal: "Vas a registrar la recepción del bolsín N° X con N documentaciones en N remitos. Esta acción actualiza sus estados." Botones **Confirmar** / *No confirmar* (A6 → mensaje "No se registró la recepción" y vuelve al inicio).

**Paso 4 – Resultado**
- Pantalla de éxito (ícono ✔ en CSS/emoji), resumen: nuevo estado del bolsín, remitos y documentaciones actualizados.
- Lista "Notificaciones enviadas" (CM origen + email).
- Botón primario **Registrar otra recepción**.

**Calidad UX (checklist)**
- Todo el texto en español, claro y sin jerga técnica; mensajes de error humanos (nunca stack traces).
- Deshabilitar botones y mostrar "Procesando…" mientras hay un request en curso (evita doble envío).
- Mover el foco al título de cada paso; `<label>` en todos los campos; contraste suficiente; navegable con teclado; `role="alert"` en errores.
- Responsive (se ve bien en notebook y en celular).
- JS dividido en funciones chicas (`api.js`-style dentro de `app.js`): `api.iniciar()`, `api.filtrar()`, `render(paso)`, etc.

---

## 10. Datos semilla (`DataLoader`, solo si la BD está vacía)

- **CM:** `CMC Buenos Aires (central)`, `CM Córdoba`, `CM Mendoza`, `CM Rosario`, `CM Villa María`, `CM Paraná`. Con email ficticio.
- **TipoDocumento:** Expediente, Dictamen, Estudio médico, Carta documento.
- **Empleados (Encargados de Bolsines):** uno en CM Córdoba (**usuario por defecto**), uno en CM Mendoza, uno en CMC, uno en CM Villa María (este sin bolsines pendientes → demuestra el estado vacío).
- **Estados semilla** de Bolsín y Remito (sección 4).
- **Bolsines en estado Enviado hacia CM Córdoba:** 
  - Bolsín 1001 desde Mendoza, precinto 50001: 2 remitos, 3 documentaciones en total.
  - Bolsín 1002 desde Rosario, precinto 50002: 1 remito, 2 documentaciones.
  - Bolsín 1003 desde Villa María, precinto 50003: 2 remitos, 4 documentaciones.
- **Bolsín 1004 Enviado hacia CM Mendoza** (no debe aparecer para el usuario de Córdoba).
- **Bolsín 1005 ya Recibido en CM destino** (tampoco debe aparecer).
- Toda documentación de un bolsín Enviado debe estar en estado `EnBolsinEnviado` con su `CambioEstadoDocumentacion` actual (fechaHoraFin null) y todo bolsín con su `CambioEstadoBolsin` actual consistente.

---

## 11. Fases de trabajo y criterios de aceptación

### Fase 0 – Esqueleto
Crear proyecto Maven/Spring Boot, `application.properties` (H2 archivo, consola habilitada, `ddl-auto=update`, `open-in-view=false`, `show-sql=false`), `README.md` base.
**Aceptación:** `./mvnw spring-boot:run` levanta y `/h2-console` abre.

Generar el proyecto con Spring Initializr (start.spring.io): Maven, Java 17, 
la última versión estable de la rama 3.x de Spring Boot (NO la 4.x). 
Dependencias: Web, Data JPA, Validation, H2 Database. 
Descargar el zip, que ya trae el mvnw, y usarlo como base. 
Dejar la versión fija en el pom.xml.

### Fase 1 – Entidades, estados y persistencia
Entidades JPA, jerarquía State + `EstadoConverter`, repositorios, `DataLoader`.
**Aceptación:** al arrancar se crean las tablas y los datos semilla; se ven en `/h2-console`; reiniciar la app **conserva** los datos y **no duplica** la semilla.

### Fase 2 – Lógica de dominio (POO pura) + tests unitarios
Implementar los métodos de la sección 6 en las entidades, con **Patrón Experto**.
Tests JUnit mínimos:
- `EnBolsinEnviado.aceptar` deja la documentación en `RecibidaYAceptada`, cierra el cambio anterior (`fechaHoraFin` ≠ null) y agrega uno nuevo con el responsable.
- Una transición inválida (p. ej. `Registrada.aceptar`) lanza `IllegalStateException`.
- `Bolsin.sosEnviado()` es verdadero/falso según el cambio de estado actual.
**Aceptación:** `./mvnw test` en verde.

### Fase 3 – Gestor, Pantalla (REST) y CU 29
DTOs (records), `GestorRecepcionBolsin`, `PantallaRegistrarRecepcionBolsin`, sesión simulada, `NotificadorEmail` simulado, `@RestControllerAdvice`, archivo `requests.http`.
**Aceptación (probar con `requests.http`):**
- Flujo principal completo: los estados cambian en la BD (bolsín, remitos, documentación) y quedan los `CambioEstado…` correctos (anterior cerrado, nuevo abierto, responsable = empleado activo).
- **A1** precinto inexistente → 404 con mensaje. **A2** CM origen inexistente → 404. **A6** `confirmado=false` → no cambia nada en la BD. **Cancelar** → no cambia nada.
- Llamar `confirmar` sin haber seleccionado bolsín → 409.
- Test de integración (`@SpringBootTest`) del flujo principal contra H2.

### Fase 4 – Frontend
Implementar la sección 9 completa. 
**Aceptación:** se puede hacer el flujo principal y A1/A2/A6/cancelar **solo con el navegador**, y el bolsín recibido deja de aparecer en el listado.

### Fase 5 – (OPCIONAL, solo si las fases 0–4 están sólidas) Flujos A3, A4, A5
El CU no detalla estos pasos; se implementan según la regla de negocio "Recepción de un Bolsín en una CM Destino" del ERS:
- **A3 (opción 2):** el EB marca qué documentaciones **no llegaron** → esas pasan a `NoRecibida` (`registrarNoRecepcion`), las que llegaron a `RecibidaYAceptada`; el remito queda `Recibido y Aceptado Parcial`.
- **A4 (opción 3):** el EB marca documentación que **no corresponde al destino** → `RecibidaYRechazada` (`rechazar`).
- **A5 (opción 4):** documentación a redirigir → `ParaRedirigir` (`redirigir`); válido solo cuando la CM destino es la CMC.
Habilitar las opciones en la UI solo cuando estén completas.

### Fase 6 – Cierre y documentación
Generar `README.md` (cómo correr, usuarios y datos de prueba, cómo reiniciar la BD borrando `./data`), `docs/DETALLES_IMPLEMENTACION.md`, `docs/DECISIONES.md`, `docs/GUIA_DEFENSA.md` (sección 13).
Pasar el checklist de la sección 14.

---

## 12. Desviaciones respecto de los diagramas (documentar en `docs/DECISIONES.md`)

| # | Desviación | Justificación |
|---|---|---|
| D1 | Los métodos reciben `Empleado` en lugar de `string[]` (nombre, apellido) | `responsableCE` es una asociación a `Empleado` en el modelo de dominio; guardar solo el nombre perdería integridad referencial en la BD |
| D2 | El Gestor **no** implementa `buscarEstadoDocumentacionRecibidoYAceptado()` ni mantiene `estadoDocumentacion` | El patrón State hace que el propio estado `EnBolsinEnviado` decida el siguiente. *Nota:* el diagrama de clases E2 aún los muestra en el Gestor, aunque el diagrama de secuencia E2 no los usa |
| D3 | Los métodos Gestor→Pantalla (`mostrar…`, `solicitar…`) se reemplazan por valores de retorno | Consecuencia de implementar una API REST (igual que en el ejemplo de la cátedra) |
| D4 | Pasos 1–4 y 5–7 se resuelven en una sola llamada cada uno | Evitar llamadas encadenadas innecesarias a la API (mismo criterio del ejemplo de la cátedra) |
| D5 | `Estado` (documentación) se persiste por **nombre** con un converter, no como entidad | Son objetos sin estado interno; evita duplicar filas |
| D6 | Login simulado con selector de usuario; email simulado | Fuera del foco del CU |

El agente puede agregar otras desviaciones, **siempre registradas** con su justificación.

---

## 13. Documentos a generar

**`docs/DETALLES_IMPLEMENTACION.md`** (lo pide la consigna): lenguaje (Java 17), framework (Spring Boot 3), tecnología (Web, API REST + frontend estático), base de datos (H2 en archivo), framework de persistencia (JPA/Hibernate), patrón implementado (State) y cómo correr el proyecto.

**`docs/GUIA_DEFENSA.md`** (para el estudiante, lenguaje simple):
1. Mapa "diagrama → clase → archivo" (tabla de la sección 8 con rutas reales).
2. Cómo se ve el patrón State en el código: dónde está el contexto, la clase abstracta y el estado concreto; qué pasaría con `if/switch`.
3. Recorrido sugerido de la demo (flujo principal, A1, A2, A6, cancelar) y cómo mostrar en `/h2-console` los cambios en `CambioEstadoDocumentacion`.
4. Diez preguntas probables de la defensa con respuesta corta (por qué Gestor como control, qué es Patrón Experto y dónde se aplica, por qué DTOs, qué hace `@Transactional`, qué pasa si se agrega un estado nuevo, por qué H2, etc.).
5. Las desviaciones de la sección 12.

---

## 14. Checklist final (Definition of Done)

- [ ] `./mvnw spring-boot:run` levanta sin errores en una máquina limpia (solo JDK 17+ instalado).
- [ ] Flujo principal ejecutable desde el navegador de punta a punta.
- [ ] Flujos A1, A2, A6 y cancelación funcionando.
- [ ] Los cambios quedan persistidos y se ven en `/h2-console`; reiniciar no duplica la semilla.
- [ ] `Documentacion.aceptar` **no contiene `if/switch` sobre el estado**; delega en `Estado`.
- [ ] `EnBolsinEnviado.aceptar` sigue la secuencia del diagrama E2.
- [ ] Gestor y entidades aplican Patrón Experto (el gestor no hace `getEstado()` + `if`).
- [ ] El boundary no expone entidades, solo DTOs.
- [ ] Nombres de clases y métodos coinciden con los diagramas (salvo desviaciones registradas).
- [ ] `./mvnw test` en verde.
- [ ] UI conforme a la sección 9.
- [ ] README y los tres documentos de `docs/` generados.
- [ ] Sin Lombok, sin servicios externos, sin credenciales ni claves.

---

## 15. Reglas de trabajo para el agente

1. Cumplir las decisiones de la sección 2; **no** agregar frameworks, librerías ni funcionalidades fuera del alcance (otros CU, autenticación real, Docker, etc.).
2. Código claro y comentado en español; en los métodos clave, un comentario con el **paso del CU / diagrama** al que corresponde (p. ej. `// CU28 paso 11c – patrón State`).
3. Cuando algo sea ambiguo, aplicar la opción más simple coherente con este plan y **registrarla** en `docs/DECISIONES.md`.
4. No modificar los PDFs de `/docs`.
5. Al finalizar cada fase, informar: qué se hizo, cómo verificarlo y qué sigue.
6. Priorizar **que funcione y se pueda explicar** por sobre completitud: es una defensa oral.
