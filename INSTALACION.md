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
