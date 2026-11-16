# Bolsines – CU 28: Registrar recepción de bolsín

Trabajo Práctico Integrador (PPAI) – **Diseño de Sistemas de Información**
UTN Facultad Regional Córdoba – 2026 – Curso **3K5** – **Grupo 8**

**Integrantes:**

| Legajo | Apellido y nombre | Email |
|:--:|:---|:---|
| 95763 | Bocek, Bruno | bocekbruno@gmail.com |
| 401675 | Diaz, Lautaro Nicolas | launikd@gmail.com |
| 407594 | Diaz Nievas, Fabricio | diaznievasfabricio@gmail.com |
| 400476 | Farfan, Gonzalo | farfangonza8@gmail.com |
| 409247 | Lamazón, Violeta | violelamazon@gmail.com |
| 409518 | Oberto, Oliver | oliveroberto6@gmail.com |
| 81994 | Pino Videla, Carlos Emanuel | emanuelpino42@gmail.com |
---

## 1. De qué se trata

El sistema permite al **Encargado de Bolsines** de una Comisión Médica registrar la llegada física de un bolsín postal: ve los bolsines que le fueron enviados, revisa los remitos y la documentación que contienen, confirma la recepción y el sistema actualiza los estados del bolsín, de sus remitos y de cada documentación, y notifica a las comisiones médicas de origen (CU 29, simulado).

El cambio de estado de la **Documentación** está implementado con el **patrón State (GoF)**, tal como se diseñó en la Entrega 2.

## 2. Qué está implementado

| Flujo | Estado |
|---|---|
| Flujo principal (recepción completa del bolsín, opción 1) | Implementado |
| A1 – No se encuentra el precinto ingresado | Implementado |
| A2 – No se encuentra la comisión médica de origen ingresada | Implementado |
| A6 – El usuario no confirma la recepción | Implementado |
| Cancelar el caso de uso | Implementado |
| Opciones de recepción 2, 3 y 4 (A3, A4, A5) | No implementadas. En pantalla aparecen como "Próximamente" y la API responde 501 |

## 3. Tecnologías

| Aspecto | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.5.16 (Web, Data JPA, Validation) |
| Tipo de aplicación | Web: API REST + frontend estático servido por el mismo Spring Boot |
| Base de datos | H2 en modo archivo (`./data/bolsines.mv.db`) |
| Persistencia | JPA / Hibernate |
| Frontend | HTML5, CSS3 y JavaScript sin frameworks, con Pico.css incluido localmente (no requiere internet) |
| Build | Maven con Maven Wrapper (`mvnw`) |
| Patrón de diseño | State (sobre `Documentacion`) |
| Patrones GRASP | Experto y Controlador (`GestorRecepcionBolsin`) |
| Pruebas | JUnit 5 y MockMvc |

No usa Lombok, servicios externos ni credenciales.

## 4. Requisitos

- **JDK 17 o superior** (`java -version` para comprobarlo).
- No hace falta instalar Maven: el proyecto incluye el wrapper.
- No hace falta conexión a internet para ejecutar la aplicación (la primera vez, Maven descarga las dependencias).

## 5. Cómo ejecutar

Desde la raíz del repositorio:

**Windows (PowerShell o CMD)**
```powershell
.\mvnw spring-boot:run
```

**Linux / macOS**
```bash
./mvnw spring-boot:run
```

Cuando la consola muestre `Started BolsinesApplication`:

- **Aplicación:** <http://localhost:8080>
- **Consola de la base de datos:** <http://localhost:8080/h2-console>
  - JDBC URL: `jdbc:h2:file:./data/bolsines`
  - User Name: `sa`
  - Password: *(vacía)*

La primera vez que arranca, crea las tablas y carga los datos de prueba. Si se reinicia sin borrar la carpeta `data`, los datos se conservan y no se duplican.

## 6. Usuarios y datos de prueba

La pantalla inicial es un **inicio de sesión** con usuario y contraseña. El sistema crea una `Sesion` asociada al `Usuario` y el Gestor obtiene al empleado logueado a partir de ella (Sesión → Usuario → `esTuUsuario`). Todos los empleados tienen el rol de **Encargado de Bolsines**.

| Usuario | Contraseña | Empleado | Comisión Médica | Para probar |
|---|---|---|---|---|
| **carlos** | 1234 | **Carlos Gómez** | CM Córdoba | Tiene 3 bolsines pendientes: es el usuario de la demo |
| ana | 1234 | Ana Martínez | CM Mendoza | No ve los bolsines de Córdoba (el 1004 es suyo) |
| roberto | 1234 | Roberto Rossi | CMC Buenos Aires (central) | Comisión central, sin bolsines pendientes |
| laura | 1234 | Laura Torres | CM Villa María | Sin bolsines pendientes: muestra el estado vacío |
| martin | 1234 | Martín Benítez | CM Rosario | Sin bolsines pendientes |

## 7. Recorrido sugerido de la demo

1. Iniciar sesión con **carlos** / **1234** (Carlos Gómez).
2. **Flujo principal:** entrar a *Registrar recepción*, seleccionar el bolsín del precinto `50001`, revisar remitos y documentación, elegir "El contenido del bolsín es igual al registrado", confirmar y ver el resultado con las notificaciones. El bolsín deja de aparecer en la lista.
3. **A1:** filtrar por un precinto inexistente (por ejemplo `99999`).
4. **A2:** filtrar por una comisión médica de origen que no tenga bolsines.
5. **A6:** llegar a la confirmación y elegir *No confirmar*: no cambia nada.
6. **Cancelar:** volver desde cualquier paso con *Cancelar*.
7. **Estado vacío:** cerrar sesión e ingresar como **laura** / **1234** (Laura Torres).

## 8. Cómo ver los cambios en la base de datos

Después de confirmar una recepción, abrir la consola H2 y ejecutar:

```sql
SELECT ID, DOCUMENTACION_ID, ESTADO, FECHA_HORA_INICIO, FECHA_HORA_FIN, RESPONSABLE_CE
FROM CAMBIOS_ESTADO_DOCUMENTACION
ORDER BY DOCUMENTACION_ID, ID;
```

Para cada documentación del bolsín recibido se ve el estado anterior (`EnBolsinEnviado`) con su fecha de fin completa, y el estado nuevo (`RecibidaYAceptada`) con `FECHA_HORA_FIN` en `NULL`. La columna `RESPONSABLE_CE` guarda al responsable con el formato `nombre|apellido`.

## 9. Pruebas automáticas

```powershell
.\mvnw test
```

Ejecuta las pruebas unitarias del dominio y del patrón State, pruebas del gestor con mocks y pruebas de integración de la API REST con MockMvc. El resultado esperado es `BUILD SUCCESS` con **51 tests** y 0 fallos.

## 10. Cómo reiniciar los datos de prueba

> [!IMPORTANT]
> Al actualizar a esta versión con el Gestor con estado y la persistencia del patrón State por nombre de clase, es necesario borrar la carpeta `data/` antes de iniciar para recrear la base de datos limpia.

1. Detener la aplicación (`Ctrl + C`).
2. Borrar la carpeta de datos:
   ```powershell
   Remove-Item -Recurse -Force .\data
   ```
3. Volver a ejecutar `.\mvnw spring-boot:run`. Se vuelven a crear las tablas y los datos.

## 11. API REST

Todas las respuestas de error tienen la forma `{ "mensaje": "texto legible" }`.

| Método y ruta | Descripción |
|---|---|
| `POST /api/sesion/login` | Inicia sesión con `{"usuario":"...","password":"..."}`. Sin sesión activa previa, crea una nueva. |
| `POST /api/sesion/logout` | Cierra la sesión activa (registra `fechaHoraFin`). Devuelve 200. |
| `GET /api/sesion` | Datos del usuario con sesión activa. Sin sesión activa devuelve 404. |
| `GET /api/comisiones-medicas` | Catálogo para el filtro de origen |
| `GET /api/recepcion-bolsin/pendientes` | Solo lectura: cantidad de bolsines enviados a la CM del empleado en sesión. Devuelve 200 con `{"cantidad": N}` (0 si no hay pendientes). Sin sesión activa devuelve 400. |
| `POST /api/recepcion-bolsin/iniciar` | Pasos 1 a 4: CM del usuario logueado y bolsines enviados |
| `GET /api/recepcion-bolsin/bolsines` | Filtro por precinto y CM de origen (A1 y A2) |
| `POST /api/recepcion-bolsin/seleccionar-bolsin` | Remitos y documentación del bolsín |
| `POST /api/recepcion-bolsin/seleccionar-recepcion` | Resumen de la opción de recepción elegida |
| `POST /api/recepcion-bolsin/confirmar` | Registra la recepción (o A6 si `confirmado` es `false`) |
| `POST /api/recepcion-bolsin/cancelar` | Cancela el caso de uso. Devuelve 204. |

El usuario se identifica por la **sesión activa**: ningún endpoint del flujo recibe identificadores de empleado. Todos los endpoints de `/api/recepcion-bolsin` operan sobre el estado del Gestor. Códigos de respuesta: `200`, `204`, `400` (datos inválidos o credenciales incorrectas), `404` (no se encontró lo pedido), `409` (llamada fuera de secuencia o transición inválida), `500` (error interno inesperado) y `501` (opción no implementada).

## 12. Dónde está el patrón State

| Rol | Clase |
|---|---|
| Contexto | `entidades/Documentacion.java` – `aceptar(...)` delega en el estado, sin `if` ni `switch` |
| Estado (abstracta) | `entidades/estado/Estado.java` |
| Estado concreto del CU | `entidades/estado/EnBolsinEnviado.java` – `aceptar(...)` sigue la secuencia del diagrama E2 y crea `RecibidaYAceptada` |
| Persistencia del estado | `entidades/estado/EstadoConverter.java` (guarda el nombre de la clase) |

El responsable de cada cambio de estado viaja como `String[]` `[nombre, apellido]` y se guarda con `ResponsableConverter`.

## 13. Estructura del proyecto

```
bolsines-cu28/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/            # Maven Wrapper
├── PLAN_IMPLEMENTACION_CU28.md      # Plan de implementación del caso de uso
├── requests.http                    # Llamadas de prueba a la API
├── README.md
├── docs/                            # Documentación y diagramas
├── src/main/java/ar/edu/utn/frc/dsi/bolsines/
│   ├── BolsinesApplication.java
│   ├── boundary/                    # Pantalla (REST), SesionController,
│   │   │                            # ComisionMedicaController, ManejadorExcepciones
│   │   ├── dto/                     # DTOs (records)
│   │   └── excepciones/             # Excepciones de negocio de la API
│   ├── control/                     # GestorRecepcionBolsin, SesionService,
│   │                                # ConsultasService
│   ├── entidades/                   # Bolsin, Remito, DetalleRemito, Documentacion,
│   │   │                            # CambioEstadoBolsin, CambioEstadoDocumentacion,
│   │   │                            # ComisionMedica, Empleado, Usuario, Sesion,
│   │   │                            # TipoDocumento, EstadoBolsin, EstadoRemito,
│   │   │                            # ResponsableConverter
│   │   └── estado/                  # Patrón State: Estado, estados concretos y EstadoConverter
│   ├── repositorios/                # Spring Data JPA
│   ├── externos/                    # NotificadorEmail (CU 29) y su versión simulada
│   └── config/                      # DataLoader (datos de prueba)
├── src/main/resources/
│   ├── application.properties
│   └── static/                      # Frontend: index.html, css/, js/
└── src/test/java/...                # Pruebas unitarias y de integración
```

## 14. Documentación

| Archivo | Contenido |
|---|---|
| `docs/DETALLES_IMPLEMENTACION.md` | Lenguaje, framework, base de datos, persistencia y patrón aplicado |
| `docs/DECISIONES.md` | Decisiones y desviaciones respecto de los diagramas (D1 a D15) |
| `docs/GUIA_DEFENSA.md` | Guía para la defensa oral y preguntas probables |
| `docs/ESPECIFICACION_UX_UI_CU28.md` | Especificación de la interfaz |
| `docs/PPAI2026_3K5_G08_E1_Analisis.pdf` | Documento de análisis de la Entrega 1 |
| `docs/PPAI2026_3K5_G08_E2_Diseño.pdf` | Documento de diseño y arquitectura de la Entrega 2 |
| `docs/secuencia entrega 2.png`, `docs/diagrama de clases entrega 2.png` | Diagramas de la Entrega 2 |
| `docs/secuenciaE1.png`, `docs/clasesE1.png`, `docs/maquinaEstadoE1.png` | Diagramas de la Entrega 1 |
| `docs/caso bolsin con consignas completo.pdf` | Consigna completa del caso de uso |

## 15. Desviaciones respecto de los diagramas

Las diferencias entre el modelado y la implementación están registradas y justificadas en [`docs/DECISIONES.md`](docs/DECISIONES.md): la API REST y los DTOs en lugar de los mensajes `mostrar…`/`solicitar…`, el Gestor con estado según la indicación de la cátedra, el correo simulado, el responsable como `String[]`, y las clases de soporte fuera del Gestor (`SesionService` para el login y `ConsultasService` para el filtro de bolsines y el catálogo de comisiones), entre otras. Lo que no se rediseñó en la Entrega 2 (por ejemplo `Usuario`, `Sesion` y `Empleado`) se mantiene como en la Entrega 1.
