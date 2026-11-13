# Bolsines - Caso de Uso 28: "Registrar recepción de bolsín"

Proyecto Práctico de Aplicación Integrador (PPAI)  
**Cátedra:** Diseño de Sistemas de Información (DSI) – UTN Facultad Regional Córdoba (FRC) – 2026  
**Curso:** 3K5 – **Grupo:** 8

---

## Tecnologías utilizadas
* **Lenguaje:** Java 17+ (ejecutado con JDK 25)
* **Framework:** Spring Boot 3.5.16 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-test`)
* **Persistencia:** JPA / Hibernate con base de datos H2 en modo archivo (`./data/bolsines`)
* **Frontend:** HTML5 + CSS3 (Pico.css local) + JavaScript Vanilla (SPA simple servida por Spring Boot)
* **Build tool:** Maven con Maven Wrapper (`mvnw`)

---

## Cómo ejecutar la aplicación

### Requisitos
* JDK 17 o superior instalado y configurado en el PATH.

### Pasos
1. Abrir una terminal en la raíz del proyecto (`bolsines-cu28`).
2. Ejecutar:
   ```powershell
   .\mvnw spring-boot:run
   ```
   * **Reinicio de datos para demostración:** Para reiniciar la base de datos a su estado inicial de prueba en cualquier momento, ejecutar:
     ```powershell
     .\mvnw spring-boot:run -Dspring-boot.run.arguments=--bolsines.reset-on-start=true
     ```
3. Abrir el navegador en:
   * **Aplicación:** [http://localhost:8080](http://localhost:8080)
   * **Consola H2:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
     * JDBC URL: `jdbc:h2:file:./data/bolsines`
     * Usuario: `sa`
     * Contraseña: *(vacía)*

---

## Estructura del Proyecto
* `src/main/java/ar/edu/utn/frc/dsi/bolsines/`
  * `boundary/`: Controladores REST (Boundary / Pantalla) y DTOs (`record`).
  * `control/`: Servicios con la lógica del caso de uso (`GestorRecepcionBolsin`).
  * `entidades/`: Clases de dominio JPA (`Bolsin`, `Remito`, `DetalleRemito`, `Documentacion`, etc.).
  * `entidades/estado/`: Jerarquía del **patrón State** para `Documentacion` y `EstadoConverter`.
  * `repositorios/`: Repositorios Spring Data JPA.
  * `externos/`: Interfaces y servicios externos simulados (CU 29 Notificación por email).
  * `config/`: Configuración y carga de datos semilla (`DataLoader`).
* `src/main/resources/`
  * `application.properties`: Configuración de persistencia H2, consola y JPA.
  * `static/`: Frontend web estático.
* `docs/`: Documentación, diagramas y guías de defensa.

