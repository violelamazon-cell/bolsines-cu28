# Guía de Defensa Oral – CU 28 "Registrar recepción de bolsín"

**Cátedra:** Diseño de Sistemas de Información (DSI) – UTN Facultad Regional Córdoba (FRC)  
**Año:** 2026 | **Curso:** 3K5 | **Grupo:** 8  
**Estudiante / Equipo:** Preparación integral para la defensa oral del PPAI.

---

## 1. Mapa "Diagrama → Clase → Archivo Físico"

Esta tabla permite ubicar inmediatamente en el código cualquier elemento modelado en los diagramas de Análisis (E1) y Diseño (E2):

| Paso CU 28 | Rol / Quién | Método en Diagrama | Clase y Archivo Físico |
|:---|:---|:---|:---|
| **1.** EB elige registrar recepción | Boundary (Pantalla) | `opcRegistrarRecepcionBolsin()` → `habilitarPantalla()` | `PantallaRegistrarRecepcionBolsin.java`<br>`src/main/java/ar/edu/utn/frc/dsi/bolsines/boundary/PantallaRegistrarRecepcionBolsin.java` |
| **2.** Obtiene CM del usuario | Gestor / Empleado | `registrarRecepcionBolsin()` → `getUsuarioLogueado()` | `GestorRecepcionBolsin.java`<br>`src/main/java/ar/edu/utn/frc/dsi/bolsines/control/GestorRecepcionBolsin.java` |
| **3.** Busca bolsines "Enviado" hacia la CM | Gestor / Bolsin | `buscarBolsinesEnviados()` → `bolsin.sosEnviado()`, `bolsin.esTuCMDestino(cm)` | `GestorRecepcionBolsin.java` y `Bolsin.java`<br>`src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/Bolsin.java` |
| **4.** Muestra origen y precinto (y filtros) | Gestor / Bolsin | `buscarCMOrigen()`, `getNroPrecinto()`, `filtrarBolsines(...)` | `GestorRecepcionBolsin.java` y `Bolsin.java` |
| **5.** EB selecciona un bolsín | Pantalla → Gestor | `tomarSeleccionBolsin(numeroBolsin)` | `PantallaRegistrarRecepcionBolsin.java` y `GestorRecepcionBolsin.java` |
| **6.** Obtiene remitos y documentaciones | Gestor / Remito / Doc | `buscarRemitosBolsin()` → `remito.getNroRemito()`, `detalle.buscarDocumentacionRemito()`, `doc.buscarTipoDocumentacion()` | `Bolsin.java`, `Remito.java`, `DetalleRemito.java`, `Documentacion.java`<br>`src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/` |
| **7.** Muestra opciones de recepción | Gestor | Opciones provistas en DTO `SeleccionarBolsinResponse` | `GestorRecepcionBolsin.java` |
| **8.** EB selecciona opción (Opción 1) | Pantalla → Gestor | `tomarSeleccionRecepcion(numeroBolsin, opcion)` | `PantallaRegistrarRecepcionBolsin.java` y `GestorRecepcionBolsin.java` |
| **9–10.** Solicita y toma confirmación | Pantalla → Gestor | `tomarConfirmacion(numeroBolsin, opcion, confirmado)` | `PantallaRegistrarRecepcionBolsin.java` y `GestorRecepcionBolsin.java` |
| **11a.** Bolsín pasa a "Recibido en CM destino" | Gestor / Bolsin | `buscarEstadoBolsinRecibido()`, `bolsin.recibir(...)` (cierra cambio anterior y crea nuevo) | `Bolsin.java` y `CambioEstadoBolsin.java`<br>`src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/` |
| **11b.** Remitos pasan a "Recibido y Aceptado" | Gestor / Remito | `bolsin.recibirYAceptarRemitos(...)` → `remito.recibirYAceptar(...)` | `Bolsin.java` y `Remito.java` |
| **11c.** Documentaciones pasan a "Recibida y Aceptada" | **Patrón State** | `remito.recibirYAceptarDocumentacion(...)` → `doc.aceptar(...)` → `EnBolsinEnviado.aceptar(...)` | `Documentacion.java` y `EnBolsinEnviado.java`<br>`src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/estado/` |
| **12.** Notifica a CM origen (CU 29) | Gestor / Externo | `llamarCU29NotificarRecepcionDeBolsin()` → `notificadorEmail.enviar(...)` | `NotificadorEmailSimulado.java`<br>`src/main/java/ar/edu/utn/frc/dsi/bolsines/externos/` |
| **Obs. 1** Cancelación del proceso | Pantalla → Gestor | `cancelar()` → `gestor.cancelar()` | `PantallaRegistrarRecepcionBolsin.java` y `GestorRecepcionBolsin.java` |

---

## 2. El Patrón State en el Código

### ¿Dónde está cada componente?
1. **Contexto (`Documentacion`):**
   * Archivo: `src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/Documentacion.java`
   * Mantiene el atributo polimórfico `private Estado estado;`.
   * El método `aceptar(LocalDateTime fechaHoraActual, Empleado responsable)` **no tiene ningún `if` ni `switch`**:
     ```java
     public void aceptar(LocalDateTime fechaHoraActual, Empleado responsable) {
         this.estado.aceptar(this, fechaHoraActual, responsable, this.cambioEstado);
     }
     ```
2. **Clase Abstracta State (`Estado`):**
   * Archivo: `src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/estado/Estado.java`
   * Provee la firma de los eventos de negocio (`aceptar`, `rechazar`, `redirigir`, `registrarNoRecepcion`). Por defecto arroja `IllegalStateException`, protegiendo la consistencia ante transiciones indebidas.
3. **Estado Concreto Origen (`EnBolsinEnviado`):**
   * Archivo: `src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/estado/EnBolsinEnviado.java`
   * Ejecuta la secuencia exacta de 6 pasos modelada en el diagrama E2:
     ```java
     // 1. actual = buscarEstadoActual(cambios)
     // 2. actual.setFechaHoraFin(ahora)
     // 3. nuevo = crearEstado()  --> new RecibidaYAceptada()
     // 4. cambioNuevo = crearCambioEstado(ahora, resp, nuevo)
     // 5. doc.setEstado(nuevo)
     // 6. doc.agregarCambioEstado(cambioNuevo)
     ```
4. **Estado Concreto Destino (`RecibidaYAceptada`):**
   * Archivo: `src/main/java/ar/edu/utn/frc/dsi/bolsines/entidades/estado/RecibidaYAceptada.java`

### ¿Qué pasaría si usáramos `if/switch`?
Si usáramos condicionales, `Documentacion` o el `Gestor` tendrían código como:
```java
// ANTIPATRÓN:
if (doc.getEstado().equals("En bolsín enviado")) {
    doc.setEstado("Recibida y aceptada");
    // cerrar cambio anterior, abrir nuevo...
} else if (doc.getEstado().equals("Registrada")) {
    throw new Exception(...);
}
```
* **Violación del principio Open/Closed (OCP):** Cada vez que surja un nuevo estado en el ciclo de vida documental (ej. "En auditoría" o "Extraviada"), tendríamos que modificar las clases existentes y agregar más ramas a los `if/switch`.
* **Dispersión y duplicación:** La lógica de transiciones quedaría dispersa en controladores o gestores en vez de encapsularse en el objeto responsable del estado.
* **Patrón State:** Cada estado es una clase independiente. Para agregar un estado, solo se crea una subclase nueva sin tocar las existentes.

---

## 3. Decisiones de UX y su Justificación (Descubrimiento a Interfaz)

### 3.1 Tabla "Hallazgo de Descubrimiento → Decisión de UI"

Basada en los artefactos del grupo (`docs/mapaempatia.jpeg` con Ernesto Valencia y `docs/moodboard.jpeg`):

| # | Artefacto | Hallazgo en el Descubrimiento | Decisión de Diseño en la UI | Justificación Técnica y de Usabilidad |
|:--:|:---|:---|:---|:---|
| **1** | **Mapa de Empatía** *(Piensa y siente)* | Ernesto siente gran responsabilidad pública ("Detrás de cada sobre hay un trámite o dictamen de una persona real; no puedo permitirme perder un expediente"). | **Confirmación explícita con diálogo modal (`<dialog>`) y foco en acción destructiva/segura.** | Evita confirmaciones accidentales antes de alterar la base de datos. El foco va por defecto a "No confirmar" y permite cancelar con la tecla `Esc`. |
| **2** | **Mapa de Empatía** *(Dice y hace)* | Realiza el cotejo físico remito por remito y documento por documento al abrir el bolsín de lona. | **Divulgación progresiva mediante acordeones (`<details>`) por remito.** | Permite al usuario revisar minuciosamente cada remito sin abrumarlo visualmente con decenas de filas simultáneas. |
| **3** | **Mapa de Empatía** *(Influencias/Prejuicios)* | Temor al sistema: *"Seguro es un sistema feo, lento y donde para hacer un simple registro tengo que hacer 20 clics."* | **Wizard secuencial de 4 pasos con stepper visual y un único botón primario.** | El camino feliz requiere apenas 3 clics desde la selección hasta la confirmación. No hay menús laberínticos ni pestañas confusas. |
| **4** | **Mapa de Empatía** *(Dolores)* | Frustración ante el error humano involuntario al tipear precintos o números de remito en el teclado. | **Filtros no destructivos y tabla con selección directa.** | Si el filtro no arroja resultados (A1/A2), la tabla no desaparece: se muestra un banner claro y un botón "Limpiar" para restaurar la vista completa sin perder el contexto. |
| **5** | **Mapa de Empatía** *(Ganancias)* | Necesidad de certeza inmediata y respaldo fehaciente de fecha, hora y usuario responsable. | **Pantalla de resultado con resumen exhaustivo y detalle de notificaciones.** | El Paso 4 detalla el nuevo estado del bolsín, las cantidades de remitos y documentos actualizados, y a qué CM se notificó por correo con el asunto exacto. |
| **6** | **Mood Board** *(Identidad institucional)* | Lema *"Trazabilidad transparente, documentación protegida"*, sobriedad y seriedad del ámbito de correspondencia oficial. | **Paleta cromática institucional: Azul postal (`#0B5CAD`) y Amarillo acento (`#F2B705`).** | Refleja una herramienta de trabajo institucional confiable, con tipografía del sistema sin fuentes externas y contraste accesible WCAG AA (> 4.5:1). |

### 3.2 Tabla de Patrones de GUI Implementados

| Patrón de GUI | Pantalla / Componente | Justificación |
|:---|:---|:---|
| **Wizard / Stepper** | Contenedor principal (Pasos 1 a 4) | Divide una tarea con impacto legal en pasos secuenciales digeribles, reduciendo la carga cognitiva. |
| **Master-Detalle** | Paso 1 (Tabla bolsines) → Paso 2 (Revisión) | Permite elegir un elemento macro (bolsín) y expandir toda su composición interna (remitos y documentaciones). |
| **Filtro Asistido** | Paso 1 (Filtro por precinto y CM origen) | Facilita encontrar inmediatamente el bolsín que el Encargado tiene físicamente en sus manos. |
| **Divulgación Progresiva** | Paso 2 (Acordeón `<details>` de remitos) | Muestra el detalle solo cuando el usuario necesita auditar el contenido interno de un remito específico. |
| **Selección Exclusiva** | Paso 2 (Radio cards grandes para opciones) | Opciones de recepción grandes, táctiles y claras. Las opciones 2, 3 y 4 se muestran deshabilitadas con el texto "Próximamente" (no se ocultan, para no desorientar al operador). |
| **Diálogo Modal Seguro** | Paso 3 (`<dialog>` nativo) | Interrumpe el flujo antes del commit transaccional. Foco inicial en "No confirmar", soporte de tecla `Esc`. |
| **Feedback Inmediato** | Todas las pantallas (banners `aria-live`) | El usuario nunca queda a oscuras: indicadores visuales de carga, advertencias semánticas y mensajes de error humanos (sin stack traces). |
| **Cancelación Continua** | Pasos 1 y 2 (Botón *Cancelar*) | Cumple la Observación 1 del caso de uso: el usuario puede abortar el flujo en cualquier momento previo a la confirmación sin alterar datos. |

### 3.3 Preguntas Clave de UX para la Defensa

* **¿Por qué un wizard y no una sola pantalla?**  
  Porque el registro de un bolsín implica una revisión documental crítica y una mutación irreversible de estados. Una pantalla única saturaría visualmente al usuario (lista de bolsines + detalle de remitos + opciones + confirmación). El wizard focaliza la atención en una única decisión por paso.
* **¿Por qué el foco inicial en "No confirmar"?**  
  Aplica el principio de diseño defensivo (*safe default*): si el usuario presiona involuntariamente la barra espaciadora o la tecla Enter al abrirse el modal, la acción ejecutada es la cancelación segura (A6), impidiendo que se comprometan cambios no deseados en la base de datos.
* **¿Por qué las opciones 2–4 aparecen deshabilitadas en vez de ocultarlas?**  
  Porque el usuario del dominio conoce que existen situaciones excepcionales (faltantes, rechazos o redirecciones). Ocultarlas le generaría la duda de si el sistema las contempla o si está en la pantalla equivocada. Mostrarlas con la etiqueta "Próximamente" comunica transparencia y previsibilidad.
* **¿Cómo se contempló la accesibilidad?**  
  Contraste cromático superior a 4.5:1 (WCAG AA), navegación 100% operable por teclado (Tab, Enter, Espacio, Esc), etiquetas `<label>` semánticas asociadas a cada input, `role="alert"` para anuncios en lectores de pantalla y desplazamiento programático del foco al título (`h2`) en cada cambio de paso.

---

## 4. Guion Sugerido para la Demostración en Vivo

### Escenario 1: Flujo Principal Feliz
1. Abrir `http://localhost:8080`.
2. Verificar que el usuario activo es **Carlos Gómez (CM Córdoba)** y que la tabla muestra 3 bolsines pendientes (1001, 1002, 1003).
3. Seleccionar el **Bolsín 1001** (origen CM Mendoza, precinto 50001).
4. En el Paso 2, desplegar los remitos: mostrar los tipos de documentación ("Expediente", "Dictamen") y asuntos.
5. Seleccionar la **Opción 1** ("El contenido del bolsín es igual al registrado") y presionar **Continuar**.
6. En el Paso 3, mostrar el diálogo modal. Presionar **Confirmar recepción**.
7. En el Paso 4, verificar el mensaje de éxito, los nuevos estados y las notificaciones enviadas a Mendoza (`mendoza@comisionesmedicas.gob.ar`).
8. Presionar **Registrar otra recepción**: constatar que el Bolsín 1001 ya **no figura** en la lista de pendientes.

### Escenario 2: Verificación en la Base de Datos (`/h2-console`)
1. Abrir `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/bolsines`, usuario: `sa`).
2. Ejecutar:
   ```sql
   SELECT b.numero_bolsin, eb.nombre, ceb.fecha_hora_inicio, ceb.fecha_hora_fin 
   FROM bolsines b 
   JOIN cambios_estado_bolsin ceb ON ceb.bolsin_id = b.id 
   JOIN estados_bolsin eb ON ceb.estado_id = eb.id 
   WHERE b.numero_bolsin = 1001;
   ```
   *Explicar:* Se observa que el cambio anterior ("Enviado") tiene `fecha_hora_fin` con valor, y el nuevo cambio ("Recibido en CM destino") está vigente (`fecha_hora_fin` es `NULL`).
3. Ejecutar sobre documentación:
   ```sql
   SELECT d.numero, d.estado, ced.fecha_hora_inicio, ced.fecha_hora_fin, e.nombre, e.apellido 
   FROM documentaciones d 
   JOIN cambios_estado_documentacion ced ON ced.documentacion_id = d.id 
   JOIN empleados e ON ced.responsable_ce_id = e.id 
   WHERE d.numero = 3001;
   ```
   *Explicar:* El estado cambió a `"Recibida y aceptada"` y el responsable registrado en el cambio es Carlos Gómez.

### Escenario 3: Flujos Alternativos
* **Flujo A1 (Precinto inexistente):** En el filtro de precinto, tipear `99999` y presionar Buscar. Mostrar el banner de advertencia ("No se encontró un bolsín con el número de precinto ingresado") y verificar que la tabla anterior no desaparece. Presionar *Limpiar*.
* **Flujo A6 (No confirma):** Seleccionar el Bolsín 1002, continuar al Paso 3, y presionar "No confirmar" (o presionar `Esc`). Verificar que regresa al Paso 1 con el banner: *"No se registró la recepción. No se modificó ningún dato."*
* **Cancelación (Obs. 1):** Seleccionar un bolsín y en el Paso 2 presionar *Cancelar*. Constatar que llama a `/api/recepcion-bolsin/cancelar` y retorna al inicio sin cambios.
* **Estado Vacío:** En el selector de usuario, cambiar a **Lucía Benítez (CM Villa María)**. Constatar la tarjeta de estado vacío: *"No hay bolsines enviados para tu comisión médica."*

---

## 5. Diez Preguntas Típicas de Examen y Respuestas Cortas

1. **¿Por qué se utiliza una clase Gestor y no se coloca la lógica directamente en el Controlador REST?**  
   Por el patrón GRASP **Controlador**. El controlador boundary solo atiende peticiones HTTP y serializa DTOs. El Gestor encapsula el flujo del caso de uso, coordina las entidades y asegura que las reglas de negocio sean independientes de la tecnología de presentación.
2. **¿Qué es el Patrón Experto y dónde se aplica en este caso de uso?**  
   Asigna la responsabilidad a la clase que posee la información necesaria. Por ejemplo, `Bolsin` sabe si está enviado (`bolsin.sosEnviado()`), `Documentacion` conoce su tipo (`doc.buscarTipoDocumentacion()`) y `EnBolsinEnviado` sabe cuál es su cambio actual y cómo crear el nuevo estado. El Gestor jamás hace consultas externas ni getters de datos ajenos para tomar decisiones que le corresponden al objeto.
3. **¿Por qué la capa Boundary expone DTOs y nunca Entidades JPA?**  
   Para evitar acoplar la interfaz web al modelo relacional, prevenir la fuga de datos confidenciales y erradicar problemas de serialización recursiva o fallos de carga diferida (`LazyInitializationException`) fuera de la transacción.
4. **¿Qué función cumple `@Transactional` en el Gestor?**  
   Garantiza atomicidad (ACID). El paso 11 modifica el Bolsín, múltiples Remitos y múltiples Documentaciones. Si cualquiera de esas operaciones o validaciones falla, Spring hace un *rollback* completo, impidiendo que la base de datos quede en un estado inconsistente.
5. **¿Qué sucedería si el negocio agrega un nuevo estado a la Documentación?**  
   Gracias al Patrón State, simplemente se crea una nueva clase que herede de `Estado` (ej. `EnAuditoria`) y se definen sus transiciones. No se modifica ninguna de las clases de estado existentes ni se altera el método `aceptar()` de `Documentacion` (Principio Abierto/Cerrado).
6. **¿Por qué la base de datos es H2 en modo archivo y no en memoria pura (`mem`)?**  
   Porque en modo archivo (`jdbc:h2:file:./data/bolsines`) los datos no se borran al reiniciar el servidor. Esto permite demostrar la persistencia real de las transacciones y auditar los registros en `/h2-console` tras reiniciar la aplicación.
7. **¿Por qué los métodos `seleccionar-recepcion` y `confirmar` reciben el `numeroBolsin` en el body (Desviación D8)?**  
   Porque los servicios en Spring Boot son singletons compartidos por todos los usuarios. Si el Gestor guardara el `bolsinSeleccionado` en una variable de instancia, un usuario B pisaría la selección de un usuario A (condición de carrera). Al recibir el identificador en la petición, el Gestor es *stateless* y seguro en concurrencia.
8. **¿Por qué `Estado` de `Documentacion` no es una tabla con clave foránea en la base de datos (Desviación D5)?**  
   Porque las clases del Patrón State modelan comportamiento puro y no poseen atributos de instancia mutables. Persistir su nombre canónico con un `AttributeConverter` de JPA es más eficiente, evita uniones relacionales innecesarias y mapea limpiamente la orientación a objetos con el modelo relacional.
9. **¿Por qué si falla el envío de email (CU 29) no se revierte la recepción física (Desviación D9)?**  
   Porque el objetivo primario del CU 28 (la recepción física y documental en la Comisión Médica) ya se completó con éxito. El envío de email es una notificación secundaria correspondiente a otro caso de uso (CU 29). Revertir la recepción física por una falla en la red de correo electrónico causaría una discrepancia grave entre la realidad física y el sistema.
10. **¿Cómo se asegura que no se dupliquen los datos semilla al reiniciar?**  
    El componente `DataLoader` ejecuta una consulta previa (`bolsinRepository.count() > 0`). Si la base de datos ya contiene registros, omite la siembra. Solo inicializa los datos si la base está completamente vacía o si se pasa explícitamente el flag `--bolsines.reset-on-start=true`.

