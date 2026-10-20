# Manual Técnico — SIG-Cerrajería

Este documento es para quien herede el proyecto: qué hay, cómo se corre, y por qué está armado así. No repite lo que ya está en `DEPLOYMENT.md` (eso es para producción) ni en las Fases 1-9 (esas explican el porqué de cada decisión con más detalle). Aquí está el resumen operativo.

## 1. Qué es esto

Un sistema de gestión para una cerrajería en Perú: inventario, ventas de mostrador, órdenes de reparación, servicios a domicilio, compras, caja diaria y reportes. Dos usuarios (administrador y empleado), sin facturación electrónica, todo en soles.

Un solo repo, dos carpetas:
- `backend/` — API REST (Java 25, Spring Boot 4.1, PostgreSQL 16)
- `frontend/` — SPA (React 19, TypeScript, Tailwind CSS 4, Vite 8)

## 2. Correrlo en local

**Backend:**
1. `docker compose up -d` (levanta solo Postgres, puerto 5433 — el 5432 suele estar ocupado por un Postgres nativo en Windows)
2. Abrir el proyecto en IntelliJ, correr `SigApplication`
3. Flyway aplica las migraciones solo (`V1` en adelante). Login inicial: `admin` / `admin123` (cambiarla en Ajustes → Contraseña)
4. Swagger: `http://localhost:8080/swagger-ui.html`

**Frontend:**
1. `npm install`
2. `npm run dev`
3. Abrir `http://localhost:5173` — **con `localhost`, no `127.0.0.1`** (el cookie del refresh token es `Secure`, y solo `localhost` se trata como origen confiable sin HTTPS)

Para producción (los dos contenerizados con Nginx de por medio), ver `DEPLOYMENT.md`.

## 3. Las tres reglas de negocio (y dónde viven en el código)

Todo lo demás en el sistema es CRUD normal. Estas tres son las que realmente definen el comportamiento:

| Regla | Qué exige | Dónde se aplica |
|---|---|---|
| 1 — Caja abierta | Ninguna operación con dinero (venta, adelanto, compra pagada de caja) se procesa sin una caja `ABIERTA` para el usuario en sesión | `CajaService.obtenerCajaAbiertaEntidad()` — lo llaman `VentaService`, `OrdenTrabajoService` y `CompraService` antes de tocar dinero |
| 2 — Kárdex obligatorio | Todo cambio de stock (venta, compra, ajuste manual) genera un movimiento de inventario en la misma transacción | `ProductoService.descontarStock()` / `incrementarStock()` / `ajustarStock()` — ninguno de los tres modifica `cantidad_stock` sin insertar también en `movimientos_inventario` |
| 3 — Sin entrega con saldo pendiente | Una orden de trabajo no puede pasar a `ENTREGADO` si tiene saldo sin cubrir, salvo que el pago que la liquida venga en la misma llamada | `OrdenTrabajoService.entregarConLiquidacion()` |

La importación de inventario desde CSV (`ImportacionProductosService`, `POST /api/v1/productos/importar`) no es una excepción a la regla 2: crea cada producto con `ProductoService.crear()`, así que el stock inicial entra al Kárdex con el motivo "Stock inicial importado desde archivo". Es todo o nada (una transacción, y con un solo error de validación no se crea nada), con `confirmar=false` solo revisa el archivo, y no actualiza productos existentes: un código repetido es un error. El CSV lo lee `LectorCsv`, que tolera lo que produce Excel según la PC (UTF-8 con BOM o Windows-1252, coma o punto y coma, línea `sep=`).

La planilla de cantidades (`PlanillaStockService`, `GET`/`POST /api/v1/productos/planilla`) tampoco: cada fila con cantidad pasa por `ProductoService.ajustarStock()` con el motivo "Conteo físico (planilla)" (modo `CONTEO`, reemplaza el stock) o "Mercadería recibida (planilla)" (modo `ENTRADA`, lo suma), todo o nada y con revisión previa como la importación. La descarga va en Windows-1252 con `sep=;` para que Excel la abra en columnas con cualquier configuración regional, y el código como `="..."` para que Excel no lo convierta en número o fecha.

Las compras de varios productos (`POST /api/v1/compras/lote`) registran cada línea con `CompraService.crear()` dentro de una sola transacción.

Los tests unitarios en `src/test/java/.../{caja,inventario,ordenes}` verifican estas tres reglas por separado, con mocks — no dependen de una base de datos levantada. `ventas/`, `compras/` y `domicilio/` cubren cómo cada módulo las respeta: la anulación de ventas, las compras pagadas de caja y el cobro de servicios a domicilio.

Otras operaciones con dinero, todas con la regla 1:
- **Servicio a domicilio realizado** (`ServicioDomicilioService.realizar`): cobra en la caja de quien lo realiza; con monto 0 no exige caja. Un servicio realizado es final.
- **Orden cancelada** (`OrdenTrabajoService.cancelar`): si se devuelve lo cobrado, es un egreso de la caja de quien lo devuelve y queda como `PagoOrden` de tipo `DEVOLUCION`. `ENTREGADO` y `CANCELADO` son estados finales (`EstadoOrden.FINALES`).

## 4. Estructura del backend

Paquete por feature, no por capa técnica (`inventario/`, `ventas/`, `ordenes/`, etc., cada uno con su entidad, repositorio, servicio, controlador y DTOs juntos). Módulos:

`auth` · `usuarios` · `inventario` · `clientes` · `ventas` · `ordenes` · `domicilio` · `compras` · `caja` · `reportes` · `dashboard` · `busqueda` · `asistente` · `whatsapp` · `config` · `common/audit` · `common/exception`

`asistente` es el intérprete de lenguaje natural (Claude con tool use, `AsistenteIAService`); `whatsapp` y el endpoint `/api/v1/asistente/mensaje` son canales delgados encima. Necesitan las claves de Anthropic y de Meta (variables opcionales: sin ellas el resto del sistema funciona igual).

Seguridad: JWT stateless. El access token (15 min) vive en memoria en el frontend; el refresh token (7 días) va en una cookie `httpOnly` + `Secure` + `SameSite=Strict`. `SecurityConfig` traduce el claim `role` del token a una `GrantedAuthority` de Spring, así que `@PreAuthorize("hasRole('ADMIN')")` funciona directo en los controladores.

Base de datos: Flyway controla el esquema (`ddl-auto: validate` — Hibernate nunca crea ni modifica tablas solo). Nueve migraciones:
- `V1` — esquema completo
- `V2` — usuario admin semilla
- `V3` — secuencia para `numero_orden` (no se puede usar el `id` autogenerado porque la columna es `NOT NULL` y se necesita antes del insert)
- `V4` — columnas de anulación en `ventas`
- `V5` — tabla `pagos_venta` (desglose de pago mixto)
- `V6` — teléfono de WhatsApp en usuarios
- `V7` — Yape, Plin y tarjeta por separado en `cajas_diarias` (el cierre se cuadra solo contra el efectivo)
- `V8` — fecha de cambio de contraseña (invalida las sesiones anteriores)
- `V9` — cobro de servicios a domicilio, estado `CANCELADO` en órdenes y pagos `DEVOLUCION`

Una migración nueva nunca edita una anterior: Flyway valida el checksum de las ya aplicadas y la app no arranca si cambian.

## 5. Estructura del frontend

`src/pages/` — una pantalla por archivo, 17 en total. `src/api/` — un módulo por dominio, funciones tipadas sobre un cliente Axios común (`src/api/axiosClient.ts`), que maneja el refresh automático del token en un interceptor de 401. `src/context/` — Auth, Caja (el estado de caja se necesita en la barra superior de toda la app), Toast, Theme. `src/components/ui/` — los átomos reutilizables (Button, Card, Chip, BottomSheet, etc.), siguiendo el sistema de diseño "Industry" definido en la Fase 7.

Un detalle no obvio: Tailwind CSS 4 no usa `tailwind.config.js` — los tokens de color y tipografía están en `src/index.css` con `@theme`. Si alguien busca el archivo de configuración clásico, no existe; se cambió de enfoque a mitad de este proyecto porque la versión 3 ya estaba desactualizada cuando se armó el frontend.

## 6. Cosas que quedaron pendientes, a propósito

No son bugs olvidados — son decisiones de alcance documentadas en su momento:

- **Asistente de IA y WhatsApp**: están implementados (`asistente/`, `whatsapp/`) pero no se probaron con claves reales de Anthropic y Meta.
- **HTTPS en producción**: no viene armado en el `docker-compose.yml`. El acceso desde el celular con HTTPS se resuelve con Tailscale (`backend/celular.bat`); sin HTTPS, fuera de `localhost` ni CORS ni la cookie del refresh token funcionan (detalle en `DEPLOYMENT.md`).

## 7. Correr los tests

```bash
./mvnw test          # unitarios (rápidos, sin Docker)
./mvnw verify         # incluye la integración con Testcontainers (levanta un Postgres real en un contenedor)
```

En el frontend, `npm test` corre Vitest con Testing Library: las utilidades de fechas (en hora de Perú, que `vitest.config.ts` fija con `TZ=America/Lima`), el formato de soles y los permisos por rol en pantallas (el empleado no ve lo que el backend le rechaza). Los tests van junto al código (`*.test.ts(x)` en `src/`) y el CI los corre en cada PR.

Los tests de integración (`integration/*IntegrationTest`: venta y Kárdex, contraseñas, importación, acceso desde el celular, ingresos de taller y domicilio) necesitan Docker corriendo — Testcontainers levanta y destruye el contenedor de Postgres solo, no hace falta nada manual.
