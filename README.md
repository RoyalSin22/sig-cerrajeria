# SIG-Cerrajería

## Qué es

SIG-Cerrajería es un sistema de gestión para una cerrajería en Perú. Lo hice pensando en la cerrajería de mi familia, donde lo usan dos personas: el dueño, con rol de administrador, y un empleado. Se usa desde la PC del mostrador o desde el celular, dentro o fuera del local.

Es un proyecto académico y de portafolio. Lo desarrollé siguiendo una metodología de 15 fases que va desde el análisis del negocio hasta el despliegue.

Lo que cubre:

- Inventario con Kárdex e importación masiva desde Excel (CSV).
- Ventas de mostrador en efectivo, Yape, Plin o pago mixto, con anulación.
- Órdenes de reparación del taller, con adelantos, entrega y cancelación.
- Servicios a domicilio con agenda por semana o mes. El cobro se registra al marcarlos como realizados.
- Compras a proveedores.
- Caja diaria por usuario. El cierre cuadra contra el efectivo, porque Yape y Plin no están en el cajón.
- Reportes de ingresos, servicios realizados, clientes que más dejaron, productos más vendidos y valor del inventario.
- Dos roles: el administrador maneja usuarios, inventario, compras y anulaciones; el empleado vende y atiende, y no ve esas opciones.
- Registro de auditoría y respaldos automáticos de la base de datos.

También tiene un asistente de IA que entiende mensajes como "¿cuántas llaves KW1 quedan?" o "registra una venta de 2 llaves Cantol", dentro de la app y opcionalmente por WhatsApp. Usa la API de Claude y necesita una clave de Anthropic; para WhatsApp hace falta además una app de Meta (los pasos están en WHATSAPP_SETUP.md). Sin esas claves, el resto del sistema funciona igual. El asistente está implementado, pero todavía no lo probé con claves reales.

## Con qué está hecho

- Backend: Java 25, Spring Boot 4.1, PostgreSQL 16, Flyway para las migraciones y JWT.
- Frontend: React 19, TypeScript, Tailwind CSS 4 y Vite 8. Se instala en el celular como app (PWA).
- Infraestructura: Docker Compose con Nginx adelante. La base de datos y la API solo escuchan dentro de la PC; el celular entra por Tailscale, con HTTPS en una red privada y sin exponer el sistema a internet.
- Pruebas: en el backend, tests unitarios y de integración contra un PostgreSQL real (Testcontainers); en el frontend, Vitest con Testing Library. El CI corre todo en GitHub Actions en cada PR.
- Lanzador para Windows escrito en Go: `SIG-Cerrajeria.exe` arranca todo con doble clic.

## Carpetas

```
sig-cerrajeria/
├── backend/             API REST (ver backend/README.md)
├── frontend/            la app web (ver frontend/README.md)
├── launcher/            el .exe para Windows (ver launcher/README.md)
├── INSTALACION.md       empezar por aquí
├── MANUAL_USUARIO.md    cómo se usa en el mostrador
├── MANUAL_TECNICO.md    arquitectura y cómo seguir desarrollando
├── DEPLOYMENT.md        Docker, respaldos y acceso desde otros equipos
└── WHATSAPP_SETUP.md    canal de WhatsApp (opcional)
```

## Para empezar

Empieza por [INSTALACION.md](./INSTALACION.md). Explica cómo instalarlo con Docker o prepararlo para desarrollar con IntelliJ y Node, y cómo configurar el celular.

El lanzador para Windows se descarga desde la [última versión publicada](https://github.com/RoyalSin22/sig-cerrajeria/releases/latest).

## Reglas de negocio

Hay tres reglas que el sistema no deja saltarse:

1. Sin caja abierta no se cobra nada. Ventas, adelantos, cobros a domicilio y compras pagadas con dinero del cajón exigen que quien las registra tenga su caja abierta.
2. El stock solo cambia por el Kárdex. Cada venta, compra o ajuste deja su movimiento en la misma transacción.
3. Una reparación no se entrega con deuda. Si queda saldo, se cobra en el mismo paso de la entrega.

El porqué de cada regla y dónde están en el código se explica en [MANUAL_TECNICO.md](./MANUAL_TECNICO.md).

## Licencia

Proyecto académico y de portafolio.
