# Cómo instalar y usar SIG-Cerrajería

Dos caminos. Si solo quieres **usar** el sistema (no modificar código), ve directo a la Opción A — es un solo comando. Si vas a seguir programando sobre esto, usa la Opción B.

---

## Opción A: instalación simple (para usar el sistema)

El sistema vive en **una PC** (la de casa, por ejemplo) que queda prendida mientras el local atiende. En el local solo se usan los celulares, que entran a esa PC por **Tailscale**, una red privada y cifrada entre tus propios equipos: funciona con wifi o con datos móviles, y el sistema **no** queda publicado en internet. El plan personal de Tailscale es gratis.

### Una sola vez, en la PC (unos 20 minutos)

1. **Instala tres programas** (siguiente, siguiente, aceptar):
   - **Docker Desktop**: https://www.docker.com/products/docker-desktop
   - **Git**: https://git-scm.com/download/win
   - **Tailscale**: https://tailscale.com/download/windows. Entra con una cuenta de la cerrajería (mejor un correo del negocio que uno personal).
2. **Ajusta Tailscale** (evita que se desconecte solo):
   - Ícono de Tailscale junto al reloj → **Preferences** → activa **Run unattended**.
   - En https://login.tailscale.com/admin/machines, en la fila de esta PC → **⋯** → **Disable key expiry**. Sin esto, cada 180 días hay que volver a iniciar sesión y los celulares dejan de entrar.
3. **Baja el sistema**: abre la carpeta Documentos, clic derecho → *Abrir en Terminal*, y escribe:
   ```
   git clone https://github.com/RoyalSin22/sig-cerrajeria.git
   ```
4. **Baja el lanzador** [SIG-Cerrajeria.exe](https://github.com/RoyalSin22/sig-cerrajeria/releases/latest/download/SIG-Cerrajeria.exe) y ponlo dentro de la carpeta `sig-cerrajeria`.
5. **Doble clic en `SIG-Cerrajeria.exe`.** Windows puede decir "Windows protegió su PC" (el programa no está firmado): **Más información → Ejecutar de todas formas**. La primera vez tarda unos minutos, porque descarga y arma todo. El lanzador deja configurado:
   - el sistema, funcionando;
   - que **arranque solo cada vez que prendas la PC** (sin abrir nada);
   - que la PC **no se suspenda** mientras está enchufada;
   - el acceso desde el celular por Tailscale;
   - una página con un **código QR** para el celular (también queda en `backend\celular.html`, para abrirla cuando haga falta).
6. Entra con usuario `admin` y contraseña `admin123`, y **cámbiala de inmediato** en Ajustes → Contraseña: esa contraseña está publicada en este repositorio.

### Una sola vez, en cada celular

La página `backend\celular.html` lo explica con el código QR:

1. Instala **Tailscale** desde Play Store y entra con **la misma cuenta** que en la PC.
2. En los Ajustes del celular busca **VPN** → **Tailscale** → activa **VPN siempre activa**, para no tener que abrir Tailscale cada vez.
3. Escanea el código QR con la cámara (o escribe la dirección en Chrome) y entra con tu usuario del sistema.
4. En Chrome: **⋮ → Instalar aplicación**. Desde ahí, solo se toca el ícono de la llave.

### El día a día: nada

- La PC arranca el sistema sola al iniciar sesión en Windows. Si Windows se reinicia por una actualización, basta con iniciar sesión.
- **Actualizaciones:** al prender la PC, el sistema busca una versión nueva en GitHub. Si la hay, hace un respaldo, la instala y sigue funcionando mientras tanto. Para no actualizar solo, agrega `AUTO_ACTUALIZAR=no` en `backend\.env`. Para actualizar en el momento: `backend\actualizar.bat`.
- **Respaldos:** automáticos al arrancar y cada 6 horas (ver [DEPLOYMENT.md](./DEPLOYMENT.md) para guardarlos en Google Drive).
- Lo que hizo el arranque automático queda en `backend\lanzador.log`, por si algo no funciona.

| Archivo | Cuándo usarlo |
|---|---|
| `SIG-Cerrajeria.exe` | La primera vez, o para abrir el sistema en esta PC |
| `backend\celular.html` | Para conectar un celular nuevo (código QR) |
| `backend\actualizar.bat` | Para instalar una versión nueva en el momento |
| `backend\respaldar.bat` | Un respaldo en el momento |
| `backend\restaurar.bat` | Solo si necesitas volver a un respaldo anterior |
| `backend\detener.bat` | Apaga el sistema (con respaldo de cierre). Los celulares no podrán entrar hasta volver a abrirlo |

Bueno saber:
- **La PC tiene que estar prendida y con la sesión de Windows iniciada.** Si quieres que inicie sesión sola tras un reinicio, configura el inicio de sesión automático de Windows (opcional).
- Si un celular se pierde o un empleado deja de trabajar contigo: en https://login.tailscale.com/admin/machines quita ese celular (**⋯ → Remove**) y, en el sistema, desactiva su usuario.
- Para que no arranque solo al prender la PC, borra `SIG-Cerrajeria.cmd` de la carpeta Inicio de Windows (tecla Windows + R → `shell:startup`).

---

## Opción B: instalación para seguir desarrollando

Necesitas instalar, en tu PC:

| Herramienta | Para qué | Link |
|---|---|---|
| **Docker Desktop** | Solo para la base de datos PostgreSQL | docker.com |
| **JDK 25** (Eclipse Temurin) | Correr el backend | adoptium.net |
| **IntelliJ IDEA** | Editar y correr el backend | jetbrains.com |
| **Node.js 24 LTS** | Correr el frontend | nodejs.org |

Clona el repo una vez (`git clone https://github.com/TU-USUARIO/sig-cerrajeria.git`) y ábrelo completo en IntelliJ — tiene `backend/` y `frontend/` adentro.

### Backend

1. Desde `sig-cerrajeria/backend/`: `cp .env.example .env` (una sola vez; Compose exige `JWT_SECRET` aunque solo levantes Postgres) y luego `docker compose up -d postgres` (esto solo levanta Postgres, no el backend — es el mismo `docker-compose.yml` de la Opción A, pero acá lo usamos distinto).

   > Si vienes de la Opción A y ya tienes los 3 contenedores corriendo, bájalos primero con `docker compose down`, porque si no el puerto de Postgres va a chocar.

2. En IntelliJ, corre `SigApplication`. Flyway arma la base de datos solo.
3. Backend en `http://localhost:8080`, Swagger en `http://localhost:8080/swagger-ui.html`.

### Frontend

1. Desde `sig-cerrajeria/frontend/`: `npm install`
2. `npm run dev`
3. Abre **http://localhost:5173** — con `localhost`, no `127.0.0.1` (si no, el refresco de sesión no funciona).

Login: `admin` / `admin123`.

---

## Ya entré, ¿ahora qué?

El **`MANUAL_USUARIO.md`** (dentro del zip del backend) explica paso a paso cómo abrir caja, vender, recibir una reparación, agendar un servicio a domicilio, y todo el uso diario — sin nada técnico.

Si algo no prende o da error, dímelo con el mensaje exacto que te sale y lo resolvemos.
