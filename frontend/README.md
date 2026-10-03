# SIG-Cerrajería — Frontend

Esqueleto inicial del frontend (Fase 9): **React 19 + Vite 8 + TypeScript + Tailwind CSS v4 + React Router 7 + Axios**, construido sobre el sistema de diseño "Industry" definido en la Fase 7 (azul acero, `Barlow Condensed`, paneles cepillados).

## Qué incluye este primer paso

- Login funcional, con el mismo flujo de JWT que ya probamos en el backend (access token en memoria + refresh por cookie httpOnly).
- Layout completo: barra superior con estado de caja siempre visible, pestañas (`Inicio · Venta · Inventario · Taller · Domicilio`), menú lateral.
- **Dashboard** funcionando de punta a punta contra `GET /api/v1/dashboard`.
- **Caja** funcionando de punta a punta (abrir/cerrar), para poder probar el resto del sistema.
- El resto de las pantallas (Venta, Inventario, Taller, Domicilio, Clientes, Compras, Reportes) están como placeholders — se completan en los siguientes pasos, igual que hicimos módulo por módulo en el backend.

## ⚠️ Importante: usa `localhost`, no `127.0.0.1`

El refresh token viaja en una cookie `Secure`. Los navegadores solo tratan `localhost` (no `127.0.0.1`) como "origen seguro" sin HTTPS. Por eso:
- El backend debe estar accesible en `http://localhost:8080` (ya lo está, según `application.yml`)
- El frontend debe abrirse en `http://localhost:5173` (Vite ya usa ese puerto por defecto)

Si accedes a alguno por `127.0.0.1`, el login funcionará pero el refresh automático fallará.

## Cómo levantarlo

1. **Backend corriendo primero** (Docker Compose + `SigApplication` desde IntelliJ, como ya lo tienes).

2. **Instalar dependencias**:
   ```bash
   npm install
   ```

3. **Levantar el servidor de desarrollo**:
   ```bash
   npm run dev
   ```
   Abre `http://localhost:5173`.

4. **Iniciar sesión** con `admin` / `admin123` (o el usuario que hayas creado).

## Variables de entorno

Copia `.env.example` a `.env` si necesitas apuntar a un backend distinto de `http://localhost:8080/api/v1`:
```bash
cp .env.example .env
```

## Próximos pasos

Construir, en este orden, las pantallas reales de: **Venta rápida** (la más crítica, según Fase 7), **Inventario**, **Clientes**, **Taller (Órdenes de trabajo)**, **Domicilio**, **Compras**, **Reportes** — cada una consumiendo los endpoints que ya están completos y probados en el backend.
