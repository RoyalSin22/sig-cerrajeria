# SIG-Cerrajería: backend

API REST del sistema, hecha con Spring Boot 4.1 sobre Java 25 y PostgreSQL 16. Flyway maneja el esquema y la sesión va con JWT.

## Módulos

El código está ordenado por funcionalidad y no por capa: cada carpeta tiene su entidad, repositorio, servicio, controlador y DTOs.

| Carpeta | Qué hace |
|---|---|
| `auth`, `usuarios` | Login, renovación de sesión, usuarios, roles y cambio de contraseña |
| `inventario` | Productos, Kárdex, ajustes de stock, fotos e importación desde CSV |
| `ventas` | Ventas de mostrador, pago mixto y anulación |
| `ordenes` | Órdenes del taller: adelantos, entrega y cancelación |
| `domicilio` | Agenda de servicios a domicilio y su cobro |
| `compras` | Proveedores y compras |
| `caja` | Apertura, movimientos y cierre de la caja diaria |
| `reportes`, `dashboard`, `busqueda` | Reportes de ingresos, panel de inicio y buscador |
| `asistente`, `whatsapp` | Asistente de IA (API de Claude) y su canal de WhatsApp |
| `common`, `config` | Auditoría, excepciones, seguridad y configuración |

Las reglas de negocio y las decisiones de diseño están en [MANUAL_TECNICO.md](../MANUAL_TECNICO.md).

## Cómo levantarlo para desarrollar

Necesitas Docker Desktop y el JDK 25.

```bash
cp .env.example .env              # una sola vez
docker compose up -d postgres     # solo la base, en localhost:5433
./mvnw spring-boot:run            # en Windows: mvnw.cmd spring-boot:run
```

Flyway aplica las migraciones solo al arrancar. La API queda en `http://localhost:8080` y la documentación interactiva en `http://localhost:8080/swagger-ui.html`.

Para probar el login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

`admin123` es la contraseña inicial. En una instalación real se cambia en Ajustes → Contraseña.

Para levantar el sistema completo (base, backend y frontend) como en el local, ver [INSTALACION.md](../INSTALACION.md).

## Tests

```bash
./mvnw test      # unitarios, no necesitan Docker
./mvnw verify    # además, los de integración contra un PostgreSQL real (Testcontainers)
```

## Variables de entorno

Van en `backend/.env`. `.env.example` trae todas, comentadas.

| Variable | Para qué |
|---|---|
| `JWT_SECRET` | Firma de la sesión. Obligatoria, mínimo 32 caracteres. El lanzador `.exe` la genera sola. |
| `DB_PASSWORD` | Contraseña de la base. Solo se puede cambiar antes de crearla. |
| `BACKUP_DIR`, `BACKUP_INTERVALO_HORAS`, `BACKUP_DIAS` | Respaldos automáticos (ver [DEPLOYMENT.md](../DEPLOYMENT.md)) |
| `ORIGENES_CELULAR` | Orígenes extra permitidos. Por defecto, la dirección de Tailscale (`https://*.ts.net`). |
| `ANTHROPIC_API_KEY`, `WHATSAPP_*` | Asistente de IA y WhatsApp, opcionales (ver [WHATSAPP_SETUP.md](../WHATSAPP_SETUP.md)) |
