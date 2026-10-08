# Despliegue — SIG-Cerrajería

**Fase 14 de la metodología.** Cubre el despliegue local/LAN con Docker Compose (backend + frontend + PostgreSQL). El camino de escalamiento a un servidor real (VPS) fue definido desde la Fase 1 y no requiere rediseño de arquitectura.

## Prerrequisitos

- Docker y Docker Compose instalados
- El repo `sig-cerrajeria` clonado completo (el `docker-compose.yml`, dentro de `backend/`, referencia al frontend con la ruta relativa `../frontend`)

## Arquitectura del despliegue

```
Cliente (navegador) --:8081--> frontend (Nginx: sirve el SPA, proxya /api y /uploads)
                                        |
                                        v
                                backend (Spring, :8080)
                                        |
                                        v
                                postgres (:5433)
```

El frontend llama por **rutas relativas** (`/api/v1`, `/uploads`); Nginx las reenvía internamente al backend. Esto evita CORS en producción por completo (el navegador solo ve un origen) y funciona sin importar con qué IP o dominio se acceda al sistema — a diferencia del desarrollo local, donde Vite (puerto 5173) y el backend (puerto 8080) sí son orígenes distintos y necesitan la configuración CORS de la Fase 9.

## Pasos

1. Copiar `.env.example` a `.env` (junto al `docker-compose.yml`, dentro de `sig-cerrajeria/backend/`) y completar `JWT_SECRET` (32+ caracteres — se puede generar con `openssl rand -base64 32`) y `DB_PASSWORD`.
2. Desde `sig-cerrajeria/backend/`:
   ```bash
   docker compose up -d --build
   ```
3. Verificar que los 3 contenedores estén saludables: `docker compose ps`
4. Abrir `http://localhost:8081` (o la IP del servidor en la red local, ej. `http://192.168.1.50:8081`)
5. Login con `admin` / `admin123` — **cambiar esta contraseña de inmediato** en Ajustes → Contraseña (está publicada en este repositorio). Luego crear el usuario del empleado en Ajustes → Usuarios.

Las migraciones de Flyway (`V1` en adelante) se aplican solas al arrancar el backend, igual que en desarrollo local.

## Acceso desde otros equipos: HTTPS, CORS y Tailscale

Tres cosas impiden usar el sistema desde un celular o desde otra PC entrando por `http://192.168.x.x:8081`:

1. **CORS.** nginx reenvía `/api/` al backend, y el backend ve el pedido como de otro origen (otro host y puerto), así que solo acepta los orígenes de `FRONTEND_ORIGIN` y `ORIGENES_CELULAR`. Una IP del wifi no está en la lista: **ni siquiera el login funciona** (403), y lo verifica `AccesoCelularIntegrationTest`.
2. **Cookie `Secure`.** El refresh token viaja en una cookie `Secure` (Fase 5) y los navegadores solo la envían por HTTPS o `localhost`. Sin HTTPS, la sesión duraría solo los 15 minutos del access token.
3. **PWA.** Los navegadores solo ofrecen "Instalar aplicación" en un contexto seguro (HTTPS o `localhost`).

**Solución adoptada: Tailscale** (`backend/celular.bat`, pasos en [INSTALACION.md](./INSTALACION.md)). `tailscale serve --bg 8081` publica el frontend en `https://<pc>.<red>.ts.net` con un certificado válido emitido por Tailscale, solo para los equipos de la red privada de la cerrajería (no es `funnel`: no queda expuesto a internet). `ORIGENES_CELULAR` vale `https://*.ts.net` por defecto, así que no hace falta tocar `.env`. Se eligió frente a las alternativas:

| Opción | Por qué no |
|---|---|
| IP del wifi sin HTTPS | No pasa CORS, no mantiene la sesión, no se instala, y no funciona fuera del local |
| Caddy con certificado propio en la LAN | Hay que instalar el certificado en cada celular, y tampoco funciona fuera del local |
| Cloudflare Tunnel con dominio propio | Deja el sistema público en internet, protegido solo por el login |
| VPS | Costo mensual y migrar la base y los respaldos; no hace falta mientras la PC del local quede prendida |

Nota de seguridad: los equipos de la red de Tailscale también alcanzan los otros puertos publicados por Docker en la PC (8080 del backend, 5433 de Postgres, este con contraseña). Son solo los equipos de la propia cerrajería, pero es otra razón para no compartir la cuenta de Tailscale.

## Backups de la base de datos (automáticos)

El servicio `backup` del `docker-compose.yml` respalda la base solo, sin tareas programadas de Windows. Arranca y se detiene junto con el sistema (`iniciar.bat` / `detener.bat`):

- **Al arrancar**, si el último respaldo tiene más de 6 horas.
- **Cada 6 horas** mientras el sistema está encendido.
- **Al detener el sistema** (`detener.bat`): el respaldo del cierre del día, después de cuadrar la caja.

No se usa una hora fija (ej. 3 a. m.) porque la PC del mostrador suele apagarse de noche y ese respaldo nunca ocurriría.

Los respaldos quedan en `backend/backups/` como `sig_cerrajeria_AAAAMMDD_HHMMSS.sql.gz` y se conservan 30 días. Cada archivo se verifica antes de darlo por bueno: un corte de luz a mitad de un respaldo no deja un archivo dañado con nombre válido.

### Respaldar ahora / restaurar

- **`respaldar.bat`**: hace un respaldo en el momento (por ejemplo, antes de actualizar el sistema).
- **`restaurar.bat`**: lista los respaldos, pide el nombre del archivo y confirmación, y reemplaza **todos** los datos actuales por los del respaldo. Antes guarda un respaldo del estado actual, por si te equivocas de archivo. La restauración es una sola transacción: si falla a mitad, la base queda exactamente como estaba.

Desde la línea de comandos (dentro de `backend/`):
```bash
docker compose exec backup sh /usr/local/bin/backup.sh ahora
docker compose stop backend
docker compose exec backup sh /usr/local/bin/backup.sh restaurar sig_cerrajeria_20261004_181128.sql.gz
docker compose start backend
```

### ⚠️ Copia fuera de esta PC

Un respaldo en el mismo disco **no protege** contra que el disco se dañe o la PC se pierda. Para eso, apunta los respaldos a una carpeta sincronizada con la nube (Google Drive, OneDrive o Dropbox para escritorio) con esta línea en `backend/.env`:
```
BACKUP_DIR=C:/Users/TU_USUARIO/Google Drive/sig-cerrajeria-backups
```
También se pueden ajustar `BACKUP_INTERVALO_HORAS` (por defecto `6`) y `BACKUP_DIAS` (por defecto `30`). Después de cambiar `.env`, reinicia con `detener.bat` + `iniciar.bat`.

## Escalamiento futuro (ya definido en Fase 1, sin cambios de arquitectura)

Migrar este mismo `docker-compose.yml` a un VPS (ej. Vultr Santiago, ~US$24/mes) implica solo: copiar el proyecto, ajustar `.env` con secretos de producción, y agregar HTTPS delante (por ejemplo Caddy con Let's Encrypt; ver "Acceso desde otros equipos" arriba). La arquitectura por capas y el empaquetado en contenedores ya están preparados para ese salto.
