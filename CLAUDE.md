# CLAUDE.md

Contexto para trabajar en este repo. Lee también `MANUAL_TECNICO.md` si necesitas más profundidad — esto es el resumen rápido.

## Qué es

Sistema de gestión para una cerrajería en Perú (2 usuarios: admin + empleado). Monorepo: `backend/` (API) y `frontend/` (SPA), cada uno con su propio stack y gestor de paquetes.

## Comandos

```bash
# Backend (desde backend/)
cp .env.example .env              # una sola vez: compose exige JWT_SECRET aunque solo levantes Postgres
docker compose up -d postgres     # solo Postgres (puerto 5433); sin 'postgres' construye y levanta los 3 servicios
./mvnw spring-boot:run            # corre la app (o botón Run en IntelliJ)
./mvnw test                       # tests unitarios, rápidos
./mvnw verify                     # + *IntegrationTest vía failsafe, con Testcontainers (necesita Docker)

# Frontend (desde frontend/)
npm install
npm run dev                       # http://localhost:5173
npm run build
npm run lint
```

Login de prueba: `admin` / `admin123` (contraseña inicial de V2; en una instalación real se cambia en Ajustes → Contraseña).

Lanzador de Windows (`launcher/`, Go): `SIG-Cerrajeria.exe`, se publica solo en GitHub Releases al cambiar `launcher/` en main (`.github/workflows/lanzador.yml`). Ver `launcher/README.md`.

CI (`.github/workflows/ci.yml`) corre en cada PR: `./mvnw verify`, `npm ci` + `lint` + `build`, tests y build del lanzador, y valida `docker-compose.yml` y `backup.sh`. Si agregas un paso de verificación nuevo, súmalo ahí.

## Las 3 reglas de negocio (no las rompas sin darte cuenta)

1. **Caja abierta obligatoria**: ninguna operación con dinero (venta, adelanto de orden, compra pagada de caja) se procesa sin que `CajaService.obtenerCajaAbiertaEntidad()` encuentre una caja `ABIERTA` para el usuario en sesión. Si agregas una operación nueva que mueve dinero, llama a este método primero, y registra el ingreso/egreso con su `MetodoPago` (`sumarIngresoACajaActiva(caja, monto, metodo)`): el cierre se cuadra contra `efectivoEsperado()`, que descuenta Yape, Plin y tarjeta porque no están en el cajón. Un pago `MIXTO` se registra línea por línea según su desglose. El cobro de un servicio a domicilio (`ServicioDomicilioService.realizar`) va a la caja de quien lo realiza, y la devolución al cancelar una orden sale de la caja de quien la devuelve.
2. **Kárdex obligatorio**: todo cambio de `producto.cantidad_stock` tiene que pasar por `ProductoService.descontarStock()` / `incrementarStock()` / `ajustarStock()` — ninguno de los tres modifica el stock sin insertar también en `movimientos_inventario`. Nunca toques `cantidad_stock` directo desde otro servicio.
3. **Sin entrega con saldo pendiente**: una orden de trabajo solo pasa a `ENTREGADO` vía `OrdenTrabajoService.entregarConLiquidacion()`, que exige liquidar el saldo en la misma llamada si existe.

Los tests en `backend/src/test/java/.../{caja,inventario,ordenes}` verifican estas tres por separado; `ventas/`, `compras/` y `domicilio/` cubren cómo las respeta cada módulo.

## Convenciones

- **Backend**: paquete por feature (`ventas/`, `ordenes/`, etc.), cada uno con su entidad, repo, servicio, controlador y DTOs juntos — no por capa técnica. MapStruct para mappers. Flyway controla el esquema (`ddl-auto: validate`, Hibernate nunca crea tablas solo). Excepciones de dominio van en `common/exception/`, se capturan en `common/advice/GlobalExceptionHandler.java` como `ProblemDetail`.
- **Frontend**: una pantalla por archivo en `src/pages/`, un módulo de API por dominio en `src/api/` (funciones tipadas sobre `axiosClient.ts`). Los componentes de UI reutilizables están en `src/components/ui/`.
- **Moneda**: todo en soles (`S/`), siempre vía `Intl.NumberFormat('es-PE', { currency: 'PEN' })` (`frontend/src/utils/format.ts`). Nunca otra moneda, nunca un número pelado.
- **Asistente de IA** (`backend/.../asistente/`): el intérprete de lenguaje natural (Claude con tool use) vive separado del canal — `AsistenteIAService` es el cerebro compartido; `whatsapp/` y el endpoint `/api/v1/asistente/mensaje` son canales delgados encima. Si agregas una acción nueva, se declara en `Herramientas.java` y su caso en `AsistenteIAService`.

## Cosas que ya rompieron una vez (no las repitas)

- **`application.yml` solo puede tener UN bloque `spring:`** — un segundo bloque sobreescribe el primero en silencio (datasource, flyway, etc. desaparecen). Si agregas config de Spring, es dentro del bloque existente.
- **Tailwind CSS 4 no usa `tailwind.config.js`** — los tokens de color/tipografía están en `frontend/src/index.css` con `@theme`. No busques el archivo de configuración clásico, no existe.
- **El frontend debe abrirse en `http://localhost:5173`, no `127.0.0.1`** — el refresh token va en cookie `Secure`, y solo `localhost` cuenta como origen confiable sin HTTPS.
- **Spring Boot 4 usa Jackson 3** (`tools.jackson.databind`), no Jackson 2 (`com.fasterxml.jackson.databind`). Jackson 2 está en el classpath solo como transitiva de springdoc, pero no hay bean de su `ObjectMapper` y los conversores HTTP no lo entienden: si lo importas, la app no arranca. Solo las anotaciones (`com.fasterxml.jackson.annotation`) se mantienen.
- **`open-in-view: false`**: los métodos de servicio que devuelven DTOs mapeados desde entidades con relaciones `LAZY` necesitan `@Transactional(readOnly = true)`, o el mapper lanza `LazyInitializationException` (500). No lo pongas a nivel de clase: una escritura sin su propio `@Transactional` quedaría en una transacción de solo lectura y no se guardaría.
- **Fechas en hora de Perú, no del servidor ni UTC**: el backend fija `America/Lima` en `SigApplication.main()` (en Docker el contenedor corre en UTC y una venta de las 20:00 caía al día siguiente). En el frontend, las fechas de calendario (`"AAAA-MM-DD"`) se manejan con `src/utils/fecha.ts`: nunca `new Date('2026-10-04')` ni `toISOString()`, que corren el día.
- **`backend/scripts/backup.sh` corre en un contenedor Linux**: debe quedar con fin de línea LF (lo fuerza `.gitattributes`); con CRLF el servicio `backup` no arranca. Respaldos: ver `DEPLOYMENT.md`.
- **Detrás de nginx, el backend ve cada pedido como de otro origen**: solo acepta los de `FRONTEND_ORIGIN` y `ORIGENES_CELULAR` (default `https://*.ts.net`, Tailscale). Una IP del wifi (`http://192.168.x.x:8081`) da 403 hasta en el login.
- **Postgres (5433) y el backend (8080) se publican solo en `127.0.0.1`** en `docker-compose.yml`: no los abras a todas las redes. La contraseña por defecto de la base está en el repo y daría acceso directo a los datos desde el wifi o Tailscale.
- Variables de entorno opcionales (WhatsApp, Anthropic) van con default vacío (`${VAR:}`) en `application.yml`, nunca con `${VAR:?mensaje}` — eso vuelve la variable obligatoria y rompe el arranque (y los tests) si no está seteada.

## Qué NO está hecho (a propósito, no son bugs olvidados)

- HTTPS en el `docker-compose.yml` de producción: el acceso con HTTPS desde el celular se resuelve con Tailscale (`backend/celular.bat`), no dentro de compose (ver `DEPLOYMENT.md`)
- Reportes agregados de servicios/clientes (RF-43/44) — hoy son listados filtrados, no reportes propios
