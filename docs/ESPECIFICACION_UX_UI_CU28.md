# Especificación UX/UI – CU 28 "Registrar recepción de bolsín"

> Documento para el agente de desarrollo. Complementa la **sección 9 del PLAN_IMPLEMENTACION_CU28.md**.
> Se aplica en la **Fase 4 (Frontend)**. Leerlo completo antes de escribir HTML/CSS/JS.

## 0. Precedencia entre documentos

- **Lo funcional** (pasos del wizard, endpoints, estados de la API, flujos A1/A2/A6/cancelar): manda el **PLAN** (secciones 7 y 9).
- **Lo visual y de contenido** (colores, tipografía, textos, tamaños, componentes): manda **este documento**.
- Si hay una contradicción que no se pueda resolver con este criterio, elegí la opción más simple, registrala en `docs/DECISIONES.md` y avisame.
- Restricciones técnicas que NO cambian: HTML + CSS + JS puro, Pico.css local (sin CDN), sin frameworks JS, sin dependencias externas, sin imágenes remotas ni fuentes por CDN.

---

## 1. Contexto de uso y usuario

**Usuario principal:** Encargado de Bolsines (EB) de una Comisión Médica.

**Situación de uso (supuesta, a validar con el grupo):**
- Recibe del correo uno o varios bolsines a la vez y tiene que registrarlos uno por uno.
- Trabaja en escritorio, con el bolsín físico al lado, abriendo y controlando el contenido contra la pantalla.
- Es una tarea repetitiva, de rutina, con poco margen de error: la trazabilidad de la documentación es crítica (regla de negocio "Trazabilidad").
- No es un usuario técnico; necesita lenguaje del dominio (bolsín, remito, precinto), no jerga de sistemas.

**Implicancias de diseño:**
- Minimizar pasos y clics; el camino feliz tiene que ser rápido.
- Hacer **muy claro qué se está confirmando** antes de que el sistema modifique estados (la acción no se puede deshacer desde este CU).
- Hacer fácil el **control visual** contra el bolsín físico (tipo de documentación, asunto, cantidad por remito).
- Nunca dejarlo sin saber qué pasó: feedback explícito en cada resultado.

### 1.1 Artefactos de descubrimiento del grupo

> Resumen de los artefactos trabajados en clase (mapa de empatía y mood board) para garantizar la trazabilidad entre el descubrimiento y las decisiones de diseño de interfaz (evaluado en la defensa oral).

**Mapa de empatía (resumen del usuario Ernesto Valencia, 42 años, Encargado de Bolsines):**
- **Piensa y siente:** Gran responsabilidad por el resguardo documental ("Detrás de cada sobre hay un trámite o dictamen de una persona real; no puedo permitirme perder un expediente"). Tensión ante precintos dañados o demoras del correo. Inseguridad al cotejar el contenido físico contra los remitos digitales.
- **Ve:** Bolsines de lona azul que llegan a primera hora, precintos plásticos numerados de seguridad, remitos impresos, pilas de legajos, expedientes médicos y biblioratos de archivo.
- **Dice y hace:** Recibe los bolsines del correo, verifica y corta el precinto de seguridad numerado, coteja remito por remito y documento por documento garantizando la trazabilidad física y digital de cada envío.
- **Escucha / Influencias:** Prejuicios sobre sistemas de gestión ("seguro es un sistema feo y lento con 20 clics para un simple registro", "me va a quitar tiempo frente a la pantalla").
- **Dolores:** Reprocesos manuales por inconsistencias en el envío; frustración ante interfaces confusas; temor al error humano involuntario en la carga de números de precinto o remito.
- **Ganancias / Necesidades:** Certeza inmediata (confirmar la recepción conforme en pocos clics), trazabilidad transparente (registro fehaciente de fecha, hora y responsable), e interfaces limpias e intuitivas que prevengan errores operativos.

**Journey del usuario (etapas, emociones y oportunidades):** `COMPLETAR`

**Mood board (palabras clave, sensaciones, referencias visuales):**
- **Lema central:** *"Trazabilidad transparente, documentación protegida."*
- **Sensaciones:** Custodia pública, precisión logística, sobriedad, orden institucional, confiabilidad y tranquilidad operativa.
- **Referencias visuales:** Bolsines postales de lona azul, sobres con bordes aéreos y sellos de correspondencia, carpetas biblioratos de archivo, mapas de ruteo y seguimiento con pines de entrega, casilleros de clasificación y buzones postales.
- **Identidad cromática:** Azul postal institucional (`#0B5CAD`) como color primario de confianza y estabilidad; amarillo de correspondencia (`#F2B705`) para acentos y estados de avance; superficies neutras de archivo (`#F4F6F9`, `#FFFFFF`, `#D5DBE3`) de alto contraste y descanso visual.

> **Instrucción para el agente:** si estas secciones están completas, las decisiones visuales de las secciones 2 a 6 deben reflejar y mencionar explícitamente esos hallazgos en `docs/GUIA_DEFENSA.md` (tabla "hallazgo de descubrimiento → decisión de UI"). Si siguen en `COMPLETAR`, usá las propuestas de este documento y dejá la tabla armada con filas vacías para que el grupo la complete.

---

## 2. Principios de experiencia

1. **Claridad antes que decoración.** Interfaz sobria, institucional, pensada para trabajar.
2. **Una cosa por pantalla.** Cada paso del wizard tiene un único objetivo y un único botón primario.
3. **Control y reversibilidad.** Siempre se puede volver o cancelar antes de confirmar. La confirmación explicita el impacto.
4. **Feedback inmediato.** Todo request tiene estado visible: cargando, éxito o error.
5. **Lenguaje del dominio.** Los textos hablan como el usuario (bolsín, remito, precinto, comisión médica).
6. **Accesible por defecto.** Teclado, foco visible, contraste, `label` en todo.

---

## 3. Identidad visual

La ambientación del dominio es de **correo / logística institucional** (azul y amarillo). Propuesta sobria, sin copiar logos ni marcas.

### 3.1 Tokens de diseño (definir en `css/app.css` sobre Pico.css)

Forzar tema claro (`<html data-theme="light">`) para consistencia en la demo. Sobrescribir las variables de Pico:

```css
:root {
  /* Marca */
  --pico-primary: #0B5CAD;
  --pico-primary-background: #0B5CAD;
  --pico-primary-hover: #084A8C;
  --pico-primary-hover-background: #084A8C;
  --pico-primary-focus: rgba(11, 92, 173, 0.25);
  --pico-primary-inverse: #FFFFFF;

  /* Acento (usar con moderación: stepper activo, detalles) */
  --app-accent: #F2B705;

  /* Semánticos */
  --app-success: #1E7F4F;
  --app-success-bg: #E6F4EC;
  --app-danger: #B3261E;
  --app-danger-bg: #FCEAE8;
  --app-warning: #8A5A00;
  --app-warning-bg: #FFF4DB;
  --app-info-bg: #E8F1FA;

  /* Superficies y texto */
  --app-bg: #F4F6F9;
  --app-surface: #FFFFFF;
  --app-border: #D5DBE3;
  --app-text: #1B2430;
  --app-text-muted: #5A6675;

  /* Forma y espacio */
  --pico-border-radius: 0.5rem;
  --app-space: 1.25rem;
}
```

- Contraste mínimo **4.5:1** en todo texto (los colores propuestos lo cumplen; verificar si se modifican).
- **Nunca** comunicar estado solo con color: acompañar con ícono/emoji y texto (✔ éxito, ⚠ advertencia, ✖ error).

### 3.2 Tipografía
- Usar la **pila de fuentes del sistema** (no cargar fuentes externas): `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`.
- Título de paso: 1.6rem, peso 600. Subtítulos: 1.1rem, color `--app-text-muted`. Texto base: 1rem (16px mínimo).
- Números de negocio (N° de bolsín, precinto, remito): `font-variant-numeric: tabular-nums` para que se alineen.

### 3.3 Espaciado y layout
- Contenedor centrado, ancho máximo **960px**; fondo de página `--app-bg`, contenido en tarjetas `--app-surface` con borde `--app-border` y sombra muy suave.
- Botones con altura mínima **44px** (zona táctil), un único botón primario por pantalla; acciones secundarias como `outline`/`secondary`; *Cancelar* como enlace/botón discreto pero siempre visible.

---

## 4. Patrones de GUI y dónde se aplican (para nombrar en la defensa)

| Patrón | Dónde | Por qué |
|---|---|---|
| **Wizard + stepper (indicador de progreso)** | Estructura general, 4 pasos | Tarea secuencial con confirmación al final; reduce carga cognitiva |
| **Master–detalle / lista seleccionable** | Paso 1 → Paso 2 | Elegir un elemento de una lista y ver su detalle |
| **Filtro sobre tabla** | Paso 1 (precinto, CM origen) | Encontrar rápido entre muchos bolsines |
| **Divulgación progresiva** (`<details>`) | Remitos en Paso 2 | Mostrar el detalle solo si se necesita |
| **Selección exclusiva (radio buttons grandes)** | Opciones de recepción | Una sola opción válida; opciones no disponibles visibles pero deshabilitadas |
| **Diálogo de confirmación modal** (`<dialog>`) | Paso 3 | Acción con efecto persistente: pedir confirmación explícita |
| **Feedback inmediato** (banners, estados de carga) | Todos | El usuario siempre sabe qué pasó |
| **Estado vacío informativo** | Paso 1 sin bolsines | Explicar por qué no hay nada y qué hacer |
| **Cancelación siempre disponible** | Todos | Obs. 1 del CU |
| **Prevención de errores** (deshabilitar mientras procesa) | Botones de envío | Evitar doble registro |

---

## 5. Pantallas y estados (detalle visual y de contenido)

> La estructura funcional es la del PLAN, sección 9. Acá se agrega el detalle de estados y textos.

### Header (global)
- Izquierda: "Bolsines" (nombre del sistema). Centro/derecha: CM del usuario activo como etiqueta destacada ("CM Córdoba").
- Derecha: selector **"Usuario activo"** con una nota discreta "Simula el inicio de sesión". Al cambiar de usuario: se reinicia el wizard y se muestra un aviso breve ("Cambiaste de usuario. Se reinició el proceso.").

### Stepper
- 4 etapas: `1 Bolsín` · `2 Revisión` · `3 Confirmación` · `4 Resultado`.
- Estados: **completada** (✔, color primario), **actual** (resaltada con `--app-accent`, `aria-current="step"`), **pendiente** (gris).
- En pantallas angostas, mostrar solo "Paso 2 de 4 · Revisión".

### Paso 1 – Elegir bolsín
| Estado | Qué se ve |
|---|---|
| Cargando | Texto "Buscando bolsines…" con indicador (`aria-busy="true"`) |
| Con datos | Tabla: CM origen · N° de precinto · *Seleccionar*. Fila clickeable y con foco por teclado |
| Vacío (404 inicial) | Tarjeta informativa: "No hay bolsines enviados para tu comisión médica." + texto de apoyo "Cuando el correo entregue un bolsín, va a aparecer acá." |
| Filtro sin resultados (A1/A2) | Banner de aviso con el mensaje del servidor; **la tabla anterior se mantiene visible**; *Limpiar* restaura la lista completa |
| Error de conexión | Banner de error con botón *Reintentar* |

### Paso 2 – Revisión
- Encabezado con tarjeta resumen: Bolsín N° X · Origen · Precinto · "N remitos · N documentaciones".
- Cada remito en un `<details>` (el primero abierto): título "Remito N° X · N documentaciones"; dentro, tabla Tipo de documento · N° · Asunto.
- Bloque "¿Qué recibiste?" con 4 opciones como tarjetas-radio grandes. Las no disponibles: deshabilitadas, con la etiqueta **"Próximamente"** (no ocultar).
- Botones: *Volver* (secundario) · *Cancelar* (discreto) · **Continuar** (primario; deshabilitado hasta elegir opción).

### Paso 3 – Confirmación
- `<dialog>` modal con foco inicial en **No confirmar** (acción segura por defecto), cierre con `Esc` = no confirmar.
- Texto: "Vas a registrar la recepción del bolsín N° X, con N documentaciones en N remitos. Esto actualiza el estado del bolsín, de los remitos y de la documentación."
- Botones: **Confirmar recepción** (primario) · *No confirmar*.
- Mientras se procesa: botones deshabilitados y "Registrando…".
- **A6 (no confirmar):** cerrar el diálogo, banner informativo "No se registró la recepción. No se modificó ningún dato." y volver al Paso 1.

### Paso 4 – Resultado
- Éxito con ✔ y título "Recepción registrada".
- Resumen en lista corta: nuevo estado del bolsín, cantidad de remitos y de documentaciones actualizadas.
- Sección "Notificaciones enviadas": una línea por CM de origen (nombre + email).
- Botón primario **Registrar otra recepción** (reinicia el wizard y recarga el listado: el bolsín recién recibido ya no aparece).
- Error en la confirmación (409/500): mostrar pantalla de error clara con "No se pudo registrar la recepción. No se modificó ningún dato." y botón *Volver al inicio*.

---

## 6. Microcopy (textos)

Tono: claro, cercano, respetuoso, sin tecnicismos. Voseo rioplatense neutro en acciones ("Seleccioná", "Confirmá"), o infinitivo/imperativo neutro — **elegir uno y ser consistente en toda la UI**.

| Situación | Texto |
|---|---|
| Título Paso 1 | Bolsines pendientes de recepción |
| Subtítulo Paso 1 | Bolsines enviados a {CM del usuario} |
| Filtros | Placeholder: "N° de precinto" / "Comisión médica de origen" |
| Botones | Buscar · Limpiar · Seleccionar · Continuar · Volver · Cancelar · Confirmar recepción · No confirmar · Registrar otra recepción |
| Opción 2/3/4 deshabilitada | "Próximamente" |
| Éxito | "Recepción registrada" |
| Cancelación | "Cancelaste el proceso. No se modificó ningún dato." |
| A6 | "No se registró la recepción. No se modificó ningún dato." |
| Cargando | "Buscando bolsines…" / "Registrando…" |
| Error genérico | "Ocurrió un problema. Intentá de nuevo en unos segundos." |

- Los mensajes de error de la API (`{ "mensaje": ... }`) se muestran tal cual, sin códigos HTTP ni stack traces.

---

## 7. Responsive y accesibilidad

**Responsive**
- Diseño **mobile-friendly** pero pensado para escritorio. Breakpoint principal ≈ 640px.
- En pantallas angostas: stepper compacto, tablas con scroll horizontal dentro de su contenedor (`overflow-x: auto`), botones a ancho completo apilados, el primario primero.

**Accesibilidad (WCAG 2.1 AA como objetivo razonable)**
- `<label>` para cada campo; `aria-live="polite"` para estados de carga/éxito y `role="alert"` para errores.
- Foco visible en todos los elementos interactivos (no quitar `outline` sin reemplazo).
- Al cambiar de paso, mover el foco al título (`tabindex="-1"` + `.focus()`).
- Todo operable con teclado (tabla seleccionable con Enter/Espacio, diálogo con `Esc`).
- Respetar `prefers-reduced-motion` (sin animaciones obligatorias).
- Estructura semántica: `header`, `main`, `h1` único, `h2` por paso, `table` con `caption` o `aria-label`, `th scope`.

---

## 8. Criterios de aceptación UX (agregar al checklist de la Fase 4)

- [ ] Se completa el flujo principal en el navegador sin usar la consola ni Postman.
- [ ] Hay un único botón primario por pantalla y *Cancelar* es visible en los pasos 1 a 3.
- [ ] Todos los estados de las secciones 5 están implementados: cargando, vacío, error, filtro sin resultados.
- [ ] El diálogo de confirmación pone el foco en *No confirmar* y cierra con `Esc` sin registrar.
- [ ] Los botones se deshabilitan mientras hay un request en curso (no se puede registrar dos veces).
- [ ] Nunca se muestra un código HTTP, JSON crudo ni stack trace al usuario.
- [ ] El estado no se comunica solo con color.
- [ ] Se puede navegar y operar todo con teclado.
- [ ] Se ve bien en un ancho de 360px y en 1366px.
- [ ] Los textos coinciden con la sección 6 y el tratamiento (voseo/neutro) es consistente.
- [ ] No hay recursos externos (fuentes, CDN, imágenes remotas).

---

## 9. Trazabilidad para la defensa (agregar a `docs/GUIA_DEFENSA.md`)

Incluir una sección "Decisiones de UX y su justificación" con:
1. La tabla **hallazgo de descubrimiento → decisión de UI** (sección 1.1).
2. La tabla de **patrones de GUI** (sección 4) con la pantalla donde se ve cada uno.
3. Respuestas cortas a: *¿por qué un wizard y no una sola pantalla?*, *¿por qué el foco inicial en "No confirmar"?*, *¿por qué las opciones 2–4 aparecen deshabilitadas?*, *¿cómo se contempló la accesibilidad?*.
