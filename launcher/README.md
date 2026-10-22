# Lanzador para Windows (`SIG-Cerrajeria.exe`)

Es lo único que hay que hacer en la PC que sirve el sistema. Tiene tres modos:

**Doble clic** (la primera vez, o para abrir el sistema):
1. Busca la carpeta del sistema (el `.exe` va en la raíz de `sig-cerrajeria\` o dentro de `backend\`).
2. La primera vez crea `backend\.env` desde `.env.example` con un `JWT_SECRET` aleatorio. No toca un `.env` existente ni cambia `DB_PASSWORD` (Postgres la fija al crear la base).
3. Si Docker Desktop está cerrado, lo abre y espera a que esté listo.
4. `docker compose up -d` y espera a que el backend responda (`/actuator/health`).
5. Deja `SIG-Cerrajeria.cmd` en la carpeta Inicio de Windows (`shell:startup`), para arrancar solo al iniciar sesión. No necesita permisos de administrador.
6. `powercfg`: la PC no se suspende mientras está enchufada (solo el tiempo "enchufada"; la pantalla sí se apaga).
7. Publica el sistema en Tailscale (`tailscale serve --bg 8081`, igual que `celular.bat`) y escribe `backend\celular.html` con la dirección, un código QR y los pasos para el celular. Esa página se abre la primera vez, o si Tailscale todavía no está listo.
8. Abre `http://localhost:8081`.

**`--inicio`** (lo ejecuta Windows al iniciar sesión, minimizado): levanta el sistema y lo **actualiza** si hay cambios nuevos en `main`, salvo `AUTO_ACTUALIZAR=no` en `.env`. Sin preguntas ni navegador; todo queda en `backend\lanzador.log`.

**`--actualizar`** (`actualizar.bat`): la actualización, a pedido.

La actualización hace `git fetch` y, si hay cambios pendientes, sigue estos pasos: respaldo (`backup.sh ahora`), `git pull --ff-only` y `docker compose up -d --build`. No hace nada si la carpeta no está en `main`, o si hay archivos versionados cambiados a mano: un `pull` los pisaría o fallaría. Si la reconstrucción falla, Docker deja corriendo la versión anterior. Los servicios del `docker-compose.yml` tienen `restart: unless-stopped`, así que vuelven solos cuando Docker arranca, aunque el lanzador falle.

En modo manual, si algo falla, explica qué pasó y espera Enter en vez de cerrar la ventana.

## Descargar

GitHub → **Releases** → `SIG-Cerrajeria.exe`, o directamente:
`https://github.com/RoyalSin22/sig-cerrajeria/releases/latest/download/SIG-Cerrajeria.exe`

Se publica solo en cada cambio a `main` que toque `launcher/` (workflow `lanzador.yml`).

## Compilar a mano

Desde cualquier sistema (Go 1.24+):

```bash
cd launcher
go test ./...
go install github.com/tc-hib/go-winres@v0.3.3
go-winres make --arch amd64          # ícono y datos de versión (winres/)
GOOS=windows GOARCH=amd64 go build -trimpath -ldflags "-s -w" -o SIG-Cerrajeria.exe .
```

Sin el paso de `go-winres` el `.exe` funciona igual, solo que sin ícono.

## Por qué Go

Se compila para Windows desde Linux (CI incluido), queda un solo archivo (~6 MB; la única dependencia es `rsc.io/qr`, para el código QR) y la misma lógica corre en Linux para probarla contra Docker real. `sistema_windows.go` contiene lo único propio de Windows: abrir Docker Desktop y el navegador, la consola en UTF-8, la carpeta Inicio y `powercfg`.
