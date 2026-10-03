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
5. Login con `admin` / `admin123` — **cambiar esta contraseña de inmediato** en Ajustes → Usuarios, o directamente en la base de datos.

Las migraciones de Flyway (`V1` a `V5`) se aplican solas al arrancar el backend, igual que en desarrollo local.

## ⚠️ Limitación conocida: refresh token, PWA y HTTPS

El refresh token viaja en una cookie `Secure` (Fase 5, decisión de seguridad). Los navegadores solo envían cookies `Secure` sobre HTTPS — **con una excepción**: tratan `localhost` como origen confiable incluso sobre HTTP simple.

Esto significa:
- **Accediendo por `http://localhost:8081`**: todo funciona normal, incluido el refresco automático de sesión.
- **Accediendo por una IP de LAN (`http://192.168.x.x:8081`) o un dominio sin HTTPS**: el login funciona, pero el refresco automático de sesión **no** — cada usuario deberá volver a iniciar sesión cada ~15 minutos (la duración del access token) en vez de que se renueve solo.

El mismo límite aplica a la instalación como app (PWA, Fase 9): los navegadores solo ofrecen "Instalar" / "Agregar a pantalla de inicio" en un contexto seguro (HTTPS o `localhost`). Desde una IP de LAN sin HTTPS, el sistema funciona igual en el navegador, pero no se puede instalar con ícono propio.

Para una cerrajería con 2 usuarios en una sola PC, esto puede ser aceptable. Si se necesita acceso e instalación desde varios dispositivos en la red (celular, tablet), la solución correcta es poner HTTPS delante — por ejemplo con Caddy (certificados automáticos, configuración mínima) o un dominio + Let's Encrypt si se migra a un VPS. No se implementa en esta fase por estar fuera del alcance de un despliegue local, pero queda documentado para no repetir la sorpresa que ya tuvimos una vez con la configuración de `application.yml`.

## Backups de la base de datos

El volumen `pgdata` persiste los datos entre reinicios del contenedor. Para un backup manual:
```bash
docker exec sig-cerrajeria-db pg_dump -U sig_admin sig_cerrajeria > backup_$(date +%Y%m%d).sql
```
Restaurar:
```bash
docker exec -i sig-cerrajeria-db psql -U sig_admin sig_cerrajeria < backup_20260101.sql
```
Para un negocio real, esto debería automatizarse (cron + rotación de backups) — no está cubierto en el alcance actual del proyecto (RNF-07 lo señala como pendiente).

## Escalamiento futuro (ya definido en Fase 1, sin cambios de arquitectura)

Migrar este mismo `docker-compose.yml` a un VPS (ej. Vultr Santiago, ~US$24/mes) implica solo: copiar el proyecto, ajustar `.env` con secretos de producción, y agregar HTTPS delante (ver arriba). La arquitectura por capas y el empaquetado en contenedores ya están preparados para ese salto.
