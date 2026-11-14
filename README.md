# Sistema de Gestión de Bolsines - CU 28: "Registrar recepción de bolsín"

Proyecto Práctico de Aplicación Integrador (PPAI)  
**Cátedra:** Diseño de Sistemas de Información (DSI) – UTN Facultad Regional Córdoba (FRC) – 2026  
**Curso:** 3K5 – **Grupo:** 8

---

## 1. Descripción del Proyecto

Implementación completa del **Caso de Uso 28: "Registrar recepción de bolsín"** correspondiente al sistema de correspondencia y expedientes de Comisiones Médicas. El sistema permite al Encargado de Bolsines registrar la llegada física de un bolsín postal, verificar los remitos y documentaciones que transporta, actualizar polimórficamente los estados mediante el **Patrón State (GoF)**, y disparar las notificaciones por correo a las Comisiones Médicas de origen (CU 29 simulado).

---

## 2. Tecnologías y Stack

* **Lenguaje:** Java 17+ (compatible con JDK 25)
* **Framework Backend:** Spring Boot 3.5.16 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-test`)
* **Persistencia:** JPA / Hibernate con base de datos H2 en modo archivo (`./data/bolsines.mv.db`)
* **Patrón de Diseño GoF:** **Patrón State** aplicado sobre la clase `Documentacion` (transiciones sin `if/switch`)
* **Patrón GRASP:** Experto en Información y Controlador (`GestorRecepcionBolsin`)
* **Frontend:** Single Page Application (SPA) con HTML5 semántico, CSS3 nativo sobre framework utilitario Pico.css (distribuido localmente sin CDN ni conexión a internet) y JavaScript moderno (ES6+ Vanilla sin frameworks)
* **Build tool:** Apache Maven con Maven Wrapper (`mvnw`)
* **Calidad y Principios:** Sin Lombok (código Java explícito), sin dependencias externas ni credenciales hardcodeadas

---

## 3. Requisitos de Ejecución

* **JDK 17 o superior** instalado y configurado en la variable de entorno `JAVA_HOME` y `PATH`.
* Conexión a internet **no requerida** para la ejecución (todas las dependencias y estilos están empaquetados localmente).

---

## 4. Instrucciones para Ejecutar la Aplicación

### 4.1 Iniciar el servidor

Abrir una consola (PowerShell o CMD) en la raíz del repositorio (`c:\Users\Violeta\bolsines-cu28`) y ejecutar:

```powershell
.\mvnw spring-boot:run
```

En sistemas Linux / macOS:
```bash
./mvnw spring-boot:run
```

### 4.2 Acceder a la Interfaz Web

Una vez que la consola indique `Started BolsinesApplication`:

* **Aplicación Web:** [http://localhost:8080](http://localhost:8080)
* **Consola de Base de Datos H2:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  * **Driver Class:** `org.h2.Driver`
  * **JDBC URL:** `jdbc:h2:file:./data/bolsines`
  * **User Name:** `sa`
  * **Password:** *(dejar en blanco)*

---

## 5. Datos Semilla y Usuarios de Prueba

La base de datos se inicializa automáticamente al primer arranque mediante `DataLoader` ([src/main/java/ar/edu/utn/frc/dsi/bolsines/config/DataLoader.java](file:///c:/Users/Violeta/bolsines-cu28/src/main/java/ar/edu/utn/frc/dsi/bolsines/config/DataLoader.java)):

### 5.1 Empleados Disponibles (Selector en Header)

| ID | Empleado | Comisión Médica | Rol | Caso de prueba |
|:--:|:---|:---|:---|:---|
| **1** | **Carlos Gómez** | **CM Córdoba (CMJ-CBA)** | Encargado de Bolsines | **Usuario por defecto**. Cuenta con 3 bolsines pendientes listos para recibir (flujo principal). |
| **2** | Ana Martínez | CM Mendoza (CMJ-MZA) | Encargada de Bolsines | Destino del bolsín 1004. Permite verificar que no ve los bolsines de Córdoba. |
| **3** | Roberto Rossi | CMC Buenos Aires (CMC-CABA) | Encargado de Bolsines | Comisión central. |
| **4** | Lucía Benítez | CM Villa María (CMJ-VM) | Encargada de Bolsines | **Demostración de Estado Vacío**: no tiene bolsines pendientes hacia su CM. |

### 5.2 Bolsines Iniciales Cargados

* **Bolsín 1001** (Precinto `50001`): Origen Mendoza → Destino Córdoba. Contiene 2 remitos y 3 documentaciones (Expedientes y Dictámenes).
* **Bolsín 1002** (Precinto `50002`): Origen Rosario → Destino Córdoba. Contiene 1 remito y 2 documentaciones.
* **Bolsín 1003** (Precinto `50003`): Origen Villa María → Destino Córdoba. Contiene 2 remitos y 4 documentaciones.
* **Bolsín 1004** (Precinto `50004`): Origen Córdoba → Destino Mendoza (no visible para Carlos Gómez).
* **Bolsín 1005** (Precinto `50005`): Origen Buenos Aires → Destino Córdoba, ya en estado *Recibido en CM destino* (no visible en pendientes).

---

## 6. Cómo Reiniciar la Base de Datos

Si durante una prueba o demo se recibieron los bolsines y se desea restaurar el estado inicial, existen dos alternativas:

### Opción A: Borrar el directorio `./data` (Recomendada)
1. Detener la aplicación (`Ctrl + C` en la consola).
2. Borrar la carpeta `data/` en la raíz del proyecto:
   ```powershell
   Remove-Item -Recurse -Force .\data
   ```
3. Volver a iniciar con `.\mvnw spring-boot:run`. El `DataLoader` volverá a sembrar la base de datos completa.

### Opción B: Parámetro de resiembra en el comando
Iniciar la aplicación pasando la propiedad de reinicio:
```powershell
.\mvnw spring-boot:run -Dspring-boot.run.arguments=--bolsines.reset-on-start=true
```

---

## 7. Estructura y Rutas Reales de Archivos

```
bolsines-cu28/
├── pom.xml                                     # Configuración de dependencias Maven
├── README.md                                   # Guía general de instalación y ejecución
├── data/                                       # Almacenamiento físico de base de datos H2
├── docs/
│   ├── DETALLES_IMPLEMENTACION.md             # Memoria técnica y arquitectura
│   ├── DECISIONES.md                           # Registro de desviaciones D1 a D9
│   ├── GUIA_DEFENSA.md                         # Guía oral de defensa y preguntas de examen
│   ├── ESPECIFICACION_UX_UI_CU28.md            # Especificación de interfaz de usuario
│   ├── PLAN_IMPLEMENTACION_CU28.md             # Plan general del caso de uso
│   ├── mapaempatia.jpeg                        # Descubrimiento: Mapa de empatía
│   └── moodboard.jpeg                          # Descubrimiento: Mood board institucional
├── src/main/java/ar/edu/utn/frc/dsi/bolsines/
│   ├── BolsinesApplication.java                # Punto de entrada Spring Boot
│   ├── boundary/
│   │   ├── PantallaRegistrarRecepcionBolsin.java # Boundary REST (@RestController)
│   │   ├── SesionController.java               # Controlador de sesión simulada
│   │   ├── ManejadorExcepciones.java           # @RestControllerAdvice (DTO Error)
│   │   └── dto/                                # DTOs inmutables (Java records)
│   ├── control/
│   │   └── GestorRecepcionBolsin.java          # Controlador del CU (@Service)
│   ├── entidades/
│   │   ├── Bolsin.java                         # Entidad agregada Bolsin
│   │   ├── Remito.java                         # Entidad Remito
│   │   ├── DetalleRemito.java                  # Ítem del remito
│   │   ├── Documentacion.java                  # Contexto del Patrón State
│   │   ├── CambioEstadoDocumentacion.java      # Historial de cambios de documentación
│   │   ├── CambioEstadoBolsin.java             # Historial de cambios de bolsín
│   │   ├── ComisionMedica.java                 # Entidad Comisión Médica
│   │   ├── Empleado.java                       # Entidad Empleado
│   │   └── estado/                             # Jerarquía del Patrón State
│   │       ├── Estado.java                     # Clase base abstracta
│   │       ├── EnBolsinEnviado.java            # Estado concreto (origen CU 28)
│   │       ├── RecibidaYAceptada.java          # Estado concreto (destino CU 28)
│   │       └── EstadoConverter.java            # Converter JPA de persistencia
│   ├── repositorios/                           # Interfaces Spring Data JPA
│   ├── config/
│   │   └── DataLoader.java                     # Semillero inicial de datos
│   └── externos/
│       ├── NotificadorEmail.java               # Interfaz del CU 29
│       └── NotificadorEmailSimulado.java       # Implementación simulada del CU 29
├── src/main/resources/
│   ├── application.properties                  # Configuración de puerto, base H2 y JPA
│   └── static/                                 # Frontend SPA
│       ├── index.html                          # Vista HTML5 semántica
│       ├── css/app.css                         # Estilos y variables CSS
│       ├── css/pico.min.css                    # Framework Pico.css local
│       └── js/app.js                           # Lógica del cliente SPA
└── src/test/java/ar/edu/utn/frc/dsi/bolsines/
    ├── entidades/DominioPuroTests.java         # Tests unitarios Fase 2 (POO y State puro)
    ├── control/GestorRecepcionBolsinTests.java # Tests unitarios del Gestor con mocks
    └── RecepcionBolsinIntegrationTests.java    # Tests de integración REST Fase 3 (MockMvc)
```
