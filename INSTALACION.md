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

### Los tres botones del día a día

En `sig-cerrajeria/backend/` quedan tres archivos para usar con doble clic, sin tocar la terminal nunca más:

| Archivo | Cuándo usarlo |
|---|---|
| `iniciar.bat` | Todos los días, al abrir el local |
| `detener.bat` | Al cerrar (opcional — no pierdes información si no lo corres, pero libera recursos de la PC) |
| `actualizar.bat` | Solo cuando alguien te entregue una versión nueva del sistema |

**Para que sea aún más cómodo:** clic derecho sobre `iniciar.bat` → *Crear acceso directo*, y arrastra ese acceso directo al escritorio. Le puedes cambiar el ícono (clic derecho → Propiedades → Cambiar ícono) por cualquier imagen que tengas a mano. Desde ahí, abrir el sistema es un doble clic, como cualquier programa instalado.

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
