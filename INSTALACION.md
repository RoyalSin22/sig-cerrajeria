# Cómo instalar y usar SIG-Cerrajería

Dos caminos. Si solo quieres **usar** el sistema (no modificar código), ve directo a la Opción A — es un solo comando. Si vas a seguir programando sobre esto, usa la Opción B.

---

## Opción A: instalación simple (Docker, recomendada para usar el sistema)

### Lo único que necesitas instalar

**Docker Desktop** — https://www.docker.com/products/docker-desktop. Solo eso. No necesitas Java, Node, ni nada más para esta opción: todo corre dentro de contenedores.

### Pasos

1. Clona el repo:
   ```bash
   git clone https://github.com/TU-USUARIO/sig-cerrajeria.git
   cd sig-cerrajeria/backend
   ```

2. Copia `.env.example` a `.env`:
   ```bash
   cp .env.example .env
   ```
   Ábrelo y cambia `JWT_SECRET` por algo aleatorio de 32+ caracteres (por ejemplo, corriendo `openssl rand -base64 32` en la terminal y pegando el resultado).

3. Desde `sig-cerrajeria/backend/`, **doble clic en `actualizar.bat`** — la primera vez construye todo (tarda unos minutos: descarga imágenes, compila el backend, arma el frontend).

4. Abre **http://localhost:8081** en el navegador.

5. Entra con usuario `admin` y contraseña `admin123`. **Cámbiala de inmediato** desde Ajustes → Contraseña: esa contraseña está publicada en este repositorio.

### Abrir el sistema con doble clic: `SIG-Cerrajeria.exe`

1. Descarga **SIG-Cerrajeria.exe** desde la página del repositorio en GitHub → **Releases** (o directo: https://github.com/RoyalSin22/sig-cerrajeria/releases/latest/download/SIG-Cerrajeria.exe).
2. Cópialo dentro de la carpeta `sig-cerrajeria` (la que tiene adentro la carpeta `backend`).
3. Clic derecho sobre él → *Enviar a* → *Escritorio (crear acceso directo)*.

Desde ahí, abrir el sistema es un doble clic: si Docker Desktop está cerrado lo abre, espera a que todo esté listo y recién entonces abre el navegador. Si algo falla, te dice qué pasó y no cierra la ventana. La primera vez también crea `backend\.env` con una clave de seguridad aleatoria si no existe (el paso 2 de arriba).

La primera vez que lo abras, Windows puede mostrar **"Windows protegió su PC"**, porque el programa no está firmado digitalmente: toca **Más información → Ejecutar de todas formas**. Solo pasa la primera vez.

### Los botones del día a día

| Archivo | Cuándo usarlo |
|---|---|
| `SIG-Cerrajeria.exe` (o `backend\iniciar.bat`) | Todos los días, al abrir el local |
| `backend\detener.bat` | Al cerrar el local: además de apagar el sistema, hace el **respaldo de cierre del día** |
| `backend\respaldar.bat` | Cuando quieras un respaldo en el momento (por ejemplo, antes de actualizar) |
| `backend\restaurar.bat` | Solo si necesitas volver a un respaldo anterior |
| `backend\actualizar.bat` | Solo cuando alguien te entregue una versión nueva del sistema |
| `backend\celular.bat` | Una sola vez, para usar el sistema desde el celular (ver abajo) |

### Usar el sistema desde el celular (Tailscale)

El sistema sigue corriendo en la PC del local; el celular se conecta a ella por **Tailscale**, una red privada y cifrada entre tus propios equipos. Funciona en el local (wifi) y fuera de él (datos móviles), con HTTPS, así que la sesión se mantiene sola y la app se puede instalar con ícono. El sistema **no** queda publicado en internet: solo lo alcanzan los equipos conectados a tu cuenta de Tailscale. El plan personal de Tailscale es gratuito.

**Una sola vez, en la PC del local:**

1. Crea una cuenta en https://tailscale.com (recomendado: con un correo de la cerrajería, no uno personal) e instala Tailscale para Windows desde https://tailscale.com/download/windows. Inicia sesión con esa cuenta.
2. En el ícono de Tailscale junto al reloj → **Preferences** → activa **Run unattended** (sigue conectado aunque se cierre la sesión de Windows).
3. En https://login.tailscale.com/admin/machines, en la fila de esta PC → menú **⋯** → **Disable key expiry**. Sin esto, cada 180 días Tailscale le pide a la PC volver a iniciar sesión y los celulares dejan de conectarse.
4. Con el sistema iniciado, doble clic en **`backend\celular.bat`**. La primera vez puede mostrar un enlace para activar HTTPS en tu cuenta: ábrelo y acéptalo. Al final muestra la dirección del sistema, del tipo `https://nombre-pc.tu-red.ts.net`. Anótala.

**En cada celular Android (administrador y empleado):**

1. Instala **Tailscale** desde Play Store e inicia sesión **con la misma cuenta de la cerrajería**. Acepta el permiso de VPN que pide Android.
2. Abre Chrome y entra a la dirección que mostró `celular.bat`. Inicia sesión en el sistema con tu usuario de siempre (cada uno con el suyo: el permiso de administrador o empleado lo da el usuario del sistema, no Tailscale).
3. Toca los tres puntos → **Instalar aplicación**. Queda el ícono de la llave en el celular.

Cada vez que uses la app, Tailscale debe estar conectado en el celular (en Android puedes dejarlo siempre activo: Ajustes → Redes e Internet → VPN → engranaje de Tailscale → **VPN siempre activa**; el camino exacto cambia según la marca del celular).

Bueno saber:
- **La PC del local tiene que estar prendida y con el sistema iniciado.** Si al cerrar ejecutas `detener.bat`, el sistema se apaga y el celular no podrá entrar hasta el día siguiente. Si quieres usarlo fuera de horario, no lo detengas: el respaldo automático igual se hace cada 6 horas.
- Si un celular se pierde o el empleado deja de trabajar contigo: en https://login.tailscale.com/admin/machines quita ese celular (**⋯ → Remove**) y, en el sistema, desactiva su usuario.
- Para desactivar el acceso desde el celular en la PC: `tailscale serve reset` en una ventana de comandos.

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
