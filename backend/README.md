# SIG-Cerrajería — Backend

Esqueleto inicial del backend (Fase 8), construido sobre **Spring Boot 4.1.0 / Java 21 / PostgreSQL 16**,
según lo validado en las Fases 5 (Arquitectura) y 6 (Base de Datos).

## Qué incluye este primer paso

- Estructura de paquetes por feature (`config`, `common`, `usuarios`, `auth`).
- Migraciones Flyway con el esquema completo de la Fase 6 (`V1__init.sql`) + un usuario admin semilla (`V2__seed_admin.sql`).
- Seguridad JWT stateless (access token en memoria del frontend + refresh token en cookie httpOnly), usando las clases nativas de Spring Security (`NimbusJwtEncoder`/`NimbusJwtDecoder`) en vez de una librería externa.
- Manejo centralizado de excepciones con `ProblemDetail`.
- Swagger UI para documentación de la API.
- **Módulo funcional ya operativo:** login (`/api/v1/auth/login`) y refresh (`/api/v1/auth/refresh`).

Los módulos de negocio (inventario, ventas, caja, órdenes, etc.) se agregan en los siguientes pasos, uno por uno.

## Cómo levantarlo

1. **Levantar PostgreSQL** (Docker Desktop debe estar corriendo):
   ```bash
   docker compose up -d
   ```
   Verifica que el contenedor esté saludable:
   ```bash
   docker ps
   ```

2. **Ejecutar el backend** (desde la carpeta del proyecto, con el IDE o por línea de comandos):
   ```bash
   ./mvnw spring-boot:run
   ```
   *(En Windows, usa `mvnw.cmd spring-boot:run` — si no tienes el wrapper aún, ábrelo directamente desde IntelliJ IDEA, que lo genera automáticamente al importar el `pom.xml`.)*

   Al arrancar, Flyway aplicará automáticamente las migraciones (`V1__init.sql` y `V2__seed_admin.sql`) sobre la base de datos vacía.

3. **Probar el login** con Postman/Insomnia o `curl`:
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
   ```
   Debe devolver un `accessToken` en el cuerpo y una cookie `refresh_token` (httpOnly).

   ⚠️ **Cambia la contraseña del usuario `admin` antes de cualquier uso real.**

4. **Ver la documentación de la API**: http://localhost:8080/swagger-ui.html

## Variables de entorno importantes

| Variable | Por defecto | Descripción |
|---|---|---|
| `JWT_SECRET` | valor de ejemplo en `application.yml` | **Cambiar en cualquier entorno real** — mínimo 32 caracteres |

## Próximos pasos (siguiente mensaje en la conversación)

Construir el primer módulo de negocio real (probablemente **Productos/Inventario**, base de todo lo demás) siguiendo el mismo patrón: entidad + repositorio + DTO/Mapper + servicio + controlador, con sus pruebas.
