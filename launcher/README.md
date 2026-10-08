# Lanzador para Windows (`SIG-Cerrajeria.exe`)

Hace lo mismo que `backend/iniciar.bat`, pero sin que el usuario tenga que saber nada de Docker:

1. Busca la carpeta del sistema (el `.exe` va en la raíz de `sig-cerrajeria\` o dentro de `backend\`).
2. La primera vez crea `backend\.env` desde `.env.example` con un `JWT_SECRET` aleatorio. No toca un `.env` existente ni cambia `DB_PASSWORD` (Postgres la fija al crear la base).
3. Si Docker Desktop está cerrado, lo abre y espera a que esté listo (hasta 3 min).
4. `docker compose up -d` (la primera vez construye las imágenes: necesita internet y unos minutos).
5. Espera a que el backend responda (`/actuator/health`) y recién entonces abre `http://localhost:8081`.

Si algo falla, explica qué pasó y espera Enter en vez de cerrar la ventana.

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

Se compila para Windows desde Linux (CI incluido), queda un solo archivo sin dependencias (~6 MB) y la misma lógica corre en Linux para probarla contra Docker real. `sistema_windows.go` contiene lo único propio de Windows: abrir Docker Desktop, abrir el navegador y la consola en UTF-8.
