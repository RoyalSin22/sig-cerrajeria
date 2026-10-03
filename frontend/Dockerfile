# --- Etapa 1: build con Vite ---
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install
COPY . .

# En produccion el frontend llama a rutas RELATIVAS (/api/v1, /uploads);
# Nginx (etapa 2) las reenvia al backend. Esto evita CORS por completo y
# funciona sin importar con que IP/dominio se acceda al sistema.
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# --- Etapa 2: Nginx sirviendo los estaticos + reverse proxy al backend ---
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
