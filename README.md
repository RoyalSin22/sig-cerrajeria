# SIG-Cerrajería

Sistema de gestión para una cerrajería en Perú: inventario con Kárdex, ventas de mostrador, órdenes de reparación, servicios a domicilio, compras, caja diaria, reportes, y un asistente de IA que entiende lenguaje natural ("¿cuántas llaves KW1 quedan?", "registra una venta de 2 llaves Cantol").

Proyecto full-stack construido de punta a punta: análisis de negocio, base de datos, backend, frontend, pruebas, despliegue con Docker y documentación — las 15 fases de la metodología, más integración de IA (asistente propio + WhatsApp Business).

## Stack

| | |
|---|---|
| **Backend** | Java 25 · Spring Boot 4.1 · PostgreSQL 16 · Flyway · JWT |
| **Frontend** | React 19 · TypeScript · Tailwind CSS 4 · Vite 8 |
| **IA** | API de Claude (interpretación de lenguaje natural con tool use) |
| **Infra** | Docker Compose · Nginx (reverse proxy) · Testcontainers |

## Estructura

```
sig-cerrajeria/
├── backend/     # API REST (ver backend/README.md)
├── frontend/    # SPA (ver frontend/README.md)
├── INSTALACION.md       # cómo levantarlo (empieza aquí)
├── MANUAL_USUARIO.md    # cómo usarlo en el día a día del mostrador
├── MANUAL_TECNICO.md    # arquitectura, decisiones, cómo seguir desarrollando
├── DEPLOYMENT.md        # despliegue con Docker, limitaciones conocidas
└── WHATSAPP_SETUP.md    # activar el canal de WhatsApp (opcional)
```

## Empezar

Instalación de un comando (Docker) o para seguir desarrollando (IntelliJ + Node): **[INSTALACION.md](./INSTALACION.md)**.

## Las 3 reglas de negocio

Todo lo demás en el sistema es CRUD normal. Estas tres son las que definen el comportamiento real:

1. **Caja abierta obligatoria** — ninguna operación con dinero se procesa sin una caja abierta para el usuario en sesión.
2. **Kárdex obligatorio** — todo cambio de stock (venta, compra, ajuste) genera un movimiento de inventario en la misma transacción.
3. **Sin entrega con saldo pendiente** — una orden de trabajo no pasa a "Entregado" con saldo sin cubrir, salvo que el pago venga en la misma operación.

Más detalle de por qué y dónde, en [MANUAL_TECNICO.md](./MANUAL_TECNICO.md).

## Licencia

Proyecto académico / de portafolio.
