# SIG-Cerrajería: frontend

La app web del sistema, hecha con React 19, TypeScript, Vite 8 y Tailwind CSS 4, con React Router y Axios. Está pensada primero para el celular y se puede instalar como app (PWA).

## Cómo está organizado

| Carpeta | Qué hay |
|---|---|
| `src/pages/` | Una pantalla por archivo: Venta, Inventario, Taller, Domicilio, Caja, Reportes, Ajustes, etc. |
| `src/api/` | Un módulo por dominio, con funciones tipadas sobre `axiosClient.ts`, que renueva la sesión sola cuando vence. |
| `src/types/` | Los tipos de las respuestas del backend. |
| `src/components/ui/` | Piezas reutilizables: botones, tarjetas, chips, hojas inferiores. |
| `src/context/` | Sesión, caja abierta, avisos y tema. |
| `src/utils/` | Formato de soles (`format.ts`) y fechas en hora de Perú (`fecha.ts`). |

Tailwind CSS 4 no usa `tailwind.config.js`: los colores y las tipografías están en `src/index.css`, dentro de `@theme`.

## Cómo levantarlo para desarrollar

Primero tiene que estar corriendo el backend (ver [backend/README.md](../backend/README.md)).

```bash
npm install
npm run dev       # http://localhost:5173
```

Entra con `admin` / `admin123`, o con el usuario que hayas creado.

Abre la app en `http://localhost:5173` y no en `127.0.0.1`. La sesión se renueva con una cookie `Secure`, y sin HTTPS el navegador solo la acepta en `localhost`. Con `127.0.0.1` el login funciona, pero la sesión se corta a los 15 minutos.

Si el backend no está en `http://localhost:8080/api/v1`, copia `.env.example` a `.env` y cambia `VITE_API_BASE_URL`.

## Antes de subir cambios

```bash
npm run lint
npm run build     # revisa los tipos y genera la versión de producción
```

Es lo mismo que corre el CI en cada PR. En producción, el frontend lo sirve Nginx dentro de Docker y llama a la API por rutas relativas (`/api/v1`).
