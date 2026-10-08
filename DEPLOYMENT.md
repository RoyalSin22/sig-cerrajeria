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

## ⚠️ Limitación conocida: refresh token, PWA y HTTPS

El refresh token viaja en una cookie `Secure` (Fase 5, decisión de seguridad). Los navegadores solo envían cookies `Secure` sobre HTTPS — **con una excepción**: tratan `localhost` como origen confiable incluso sobre HTTP simple.

Esto significa:
- **Accediendo por `http://localhost:8081`**: todo funciona normal, incluido el refresco automático de sesión.
- **Accediendo por una IP de LAN (`http://192.168.x.x:8081`) o un dominio sin HTTPS**: el login funciona, pero el refresco automático de sesión **no** — cada usuario deberá volver a iniciar sesión cada ~15 minutos (la duración del access token) en vez de que se renueve solo.

El mismo límite aplica a la instalación como app (PWA, Fase 9): los navegadores solo ofrecen "Instalar" / "Agregar a pantalla de inicio" en un contexto seguro (HTTPS o `localhost`). Desde una IP de LAN sin HTTPS, el sistema funciona igual en el navegador, pero no se puede instalar con ícono propio.

Para una cerrajería con 2 usuarios en una sola PC, esto puede ser aceptable. Si se necesita acceso e instalación desde varios dispositivos en la red (celular, tablet), la solución correcta es poner HTTPS delante — por ejemplo con Caddy (certificados automáticos, configuración mínima) o un dominio + Let's Encrypt si se migra a un VPS. No se implementa en esta fase por estar fuera del alcance de un despliegue local, pero queda documentado para no repetir la sorpresa que ya tuvimos una vez con la configuración de `application.yml`.

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

Migrar este mismo `docker-compose.yml` a un VPS (ej. Vultr Santiago, ~US$24/mes) implica solo: copiar el proyecto, ajustar `.env` con secretos de producción, y agregar HTTPS delante (ver arriba). La arquitectura por capas y el empaquetado en contenedores ya están preparados para ese salto.
