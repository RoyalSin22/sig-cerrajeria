# SIG-Cerrajería

Sistema de gestión para una cerrajería en Perú. Cubre lo que pasa en el día a día del local: inventario, ventas de mostrador, reparaciones del taller, servicios a domicilio, compras a proveedores y la caja. Lo usan dos personas, el dueño y un empleado, desde la PC del mostrador o desde el celular.

También tiene un asistente que entiende mensajes escritos como uno habla, por ejemplo "¿cuántas llaves KW1 quedan?" o "registra una venta de 2 llaves Cantol". Funciona dentro de la app y, si se configura, por WhatsApp. Usa la API de Claude, así que necesita una clave de Anthropic; sin ella el resto del sistema funciona igual. Está implementado, pero todavía no se probó con claves reales.

El proyecto se hizo siguiendo una metodología de 15 fases, desde el análisis del negocio hasta el despliegue.

## Con qué está hecho

El backend es Java 25 con Spring Boot 4.1, sobre PostgreSQL 16. Flyway maneja las migraciones y la sesión va con JWT.

El frontend es React 19 con TypeScript, Tailwind CSS 4 y Vite 8. Se puede instalar en el celular como app (PWA).

Todo corre con Docker Compose, con Nginx adelante. Para entrar desde el celular, dentro o fuera del local, se usa Tailscale. Los tests de integración levantan un PostgreSQL de verdad con Testcontainers. En Windows hay un lanzador escrito en Go, `SIG-Cerrajeria.exe`, que arranca todo con doble clic.

## Carpetas

```
sig-cerrajeria/
├── backend/             # API REST (ver backend/README.md)
├── frontend/            # la app web (ver frontend/README.md)
├── launcher/            # el .exe para Windows (ver launcher/README.md)
├── INSTALACION.md       # empieza por aquí
├── MANUAL_USUARIO.md    # cómo se usa en el mostrador
├── MANUAL_TECNICO.md    # arquitectura y cómo seguir desarrollando
├── DEPLOYMENT.md        # Docker, respaldos y acceso desde otros equipos
└── WHATSAPP_SETUP.md    # el canal de WhatsApp (opcional)
```

## Para empezar

Si solo quieres usarlo, baja `SIG-Cerrajeria.exe` desde [Releases](https://github.com/RoyalSin22/sig-cerrajeria/releases/latest) y sigue [INSTALACION.md](./INSTALACION.md). Ahí también está cómo prepararlo para desarrollar (IntelliJ y Node) y cómo configurar el celular.

## Reglas de negocio

Casi todo el código son altas, bajas y consultas. Pero hay tres reglas que el sistema no deja saltarse:

1. Sin caja abierta no se cobra nada. Una venta, el adelanto de una reparación, un servicio a domicilio o una compra pagada con dinero del cajón exigen que quien la registra tenga su caja abierta.
2. El stock solo cambia por el Kárdex. Cada venta, compra o ajuste deja su movimiento de inventario, en la misma transacción.
3. Una reparación no se entrega con deuda. Si queda saldo, se cobra en el mismo paso de la entrega.

En [MANUAL_TECNICO.md](./MANUAL_TECNICO.md) está el porqué de cada una y dónde vive en el código.

## Licencia

Proyecto académico y de portafolio.
