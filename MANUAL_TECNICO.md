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

Los tests unitarios en `src/test/java/.../{caja,inventario,ordenes}` verifican estas tres reglas por separado, con mocks — no dependen de una base de datos levantada.

## 4. Estructura del backend

Paquete por feature, no por capa técnica (`inventario/`, `ventas/`, `ordenes/`, etc., cada uno con su entidad, repositorio, servicio, controlador y DTOs juntos). Módulos:

`auth` · `usuarios` · `inventario` · `clientes` · `ventas` · `ordenes` · `domicilio` · `compras` · `caja` · `reportes` · `dashboard` · `busqueda` · `common/audit` · `common/exception`

Seguridad: JWT stateless. El access token (15 min) vive en memoria en el frontend; el refresh token (7 días) va en una cookie `httpOnly` + `Secure` + `SameSite=Strict`. `SecurityConfig` traduce el claim `role` del token a una `GrantedAuthority` de Spring, así que `@PreAuthorize("hasRole('ADMIN')")` funciona directo en los controladores.

Base de datos: Flyway controla el esquema (`ddl-auto: validate` — Hibernate nunca crea ni modifica tablas solo). Cinco migraciones:
- `V1` — esquema completo
- `V2` — usuario admin semilla
- `V3` — secuencia para `numero_orden` (no se puede usar el `id` autogenerado porque la columna es `NOT NULL` y se necesita antes del insert)
- `V4` — columnas de anulación en `ventas`
- `V5` — tabla `pagos_venta` (desglose de pago mixto)

## 5. Estructura del frontend

`src/pages/` — una pantalla por archivo, 17 en total. `src/api/` — un módulo por dominio, funciones tipadas sobre un cliente Axios común (`src/api/axiosClient.ts`), que maneja el refresh automático del token en un interceptor de 401. `src/context/` — Auth, Caja (el estado de caja se necesita en la barra superior de toda la app), Toast, Theme. `src/components/ui/` — los átomos reutilizables (Button, Card, Chip, BottomSheet, etc.), siguiendo el sistema de diseño "Industry" definido en la Fase 7.

Un detalle no obvio: Tailwind CSS 4 no usa `tailwind.config.js` — los tokens de color y tipografía están en `src/index.css` con `@theme`. Si alguien busca el archivo de configuración clásico, no existe; se cambió de enfoque a mitad de este proyecto porque la versión 3 ya estaba desactualizada cuando se armó el frontend.

## 6. Cosas que quedaron pendientes, a propósito

No son bugs olvidados — son decisiones de alcance documentadas en su momento:

- **Backups automáticos de la base de datos** (RNF-07): no implementados. `DEPLOYMENT.md` trae el comando manual de `pg_dump`.
- **WhatsApp Business API y asistente de IA**: quedaron fuera del alcance del MVP desde la Fase 1. La arquitectura no les cierra el paso — se agregarían como módulos nuevos sin tocar lo existente.
- **HTTPS en producción**: no viene armado en el `docker-compose.yml` actual. Sin él, el refresh token no se renueve solo fuera de `localhost` (detalle completo en `DEPLOYMENT.md`).
- **Reportes de servicios realizados / historial agregado de clientes** (RF-43, RF-44): existen como listados filtrados, no como reportes con agregaciones propias.

## 7. Correr los tests

```bash
./mvnw test          # unitarios (rápidos, sin Docker)
./mvnw verify         # incluye la integración con Testcontainers (levanta un Postgres real en un contenedor)
```

La integración (`VentaFlowIntegrationTest`) necesita Docker corriendo — Testcontainers levanta y destruye el contenedor de Postgres solo, no hace falta nada manual.
