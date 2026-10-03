# Integración con WhatsApp (Fase 11) — cómo activarla

Esta parte del sistema está construida y lista, pero necesita credenciales que **solo tú puedes generar** (no hay forma de que esto funcione sin que pases por Meta). Es para pruebas — con la cuenta de desarrollador gratuita de Meta alcanza y sobra.

## 1. Cuenta de Meta for Developers

1. Entra a **developers.facebook.com** con tu cuenta de Facebook y crea una app de tipo "Business".
2. Dentro de la app, agrega el producto **WhatsApp**.
3. Meta te da automáticamente, sin verificar ningún negocio real:
   - Un **número de teléfono de prueba** (con límite de 5 destinatarios verificados — agrega tu propio celular como destinatario de prueba ahí mismo)
   - Un **`Phone number ID`** (un número largo, lo ves en el panel de WhatsApp → API Setup)
   - Un **token de acceso temporal** (dura 24 horas — para algo más permanente hay que configurar un "System User" en Business Manager, un paso extra que no hace falta para probar)

## 2. Tu clave de Anthropic

Entra a **console.anthropic.com**, crea una API key. Esto es lo que interpreta los mensajes ("Claude" en el código).

## 3. Variables de entorno

Agrégalas a tu `.env` (o como variables de entorno si corres el backend desde IntelliJ):

```
WHATSAPP_ACCESS_TOKEN=el token temporal de Meta
WHATSAPP_PHONE_NUMBER_ID=el Phone number ID
WHATSAPP_VERIFY_TOKEN=inventa cualquier palabra (ej. "cerrajeria2026")
ANTHROPIC_API_KEY=tu api key de Anthropic
```

`WHATSAPP_VERIFY_TOKEN` no viene de Meta — lo inventas tú y lo pones **igual** en el siguiente paso.

## 4. Exponer tu backend a internet (para pruebas)

Meta necesita una URL pública para mandarte los mensajes — no le puede pegar a tu `localhost`. Para probar, usa **ngrok**:

```bash
ngrok http 8080
```

Te da una URL como `https://algo-random.ngrok-free.app`. Esa es la que usas en el siguiente paso, **no** `localhost`.

## 5. Configurar el webhook en Meta

En el panel de WhatsApp de tu app → **Configuration** → Webhook:
- URL de callback: `https://tu-url-de-ngrok.ngrok-free.app/api/v1/whatsapp/webhook`
- Verify token: el mismo que pusiste en `WHATSAPP_VERIFY_TOKEN`
- Suscríbete al campo **`messages`**

Meta va a hacer un GET automático a esa URL para confirmar que el token coincide — si tu backend está corriendo y las variables están bien puestas, pasa solo.

## 6. Vincular tu número a un usuario del sistema

El sistema necesita saber **quién** te escribe, para que las reglas de caja/Kárdex/auditoría se apliquen correctamente. Un administrador, desde Swagger o Postman:

```
PATCH /api/v1/usuarios/{id}/whatsapp
{ "telefonoWhatsapp": "51987654321" }
```

El número va sin el `+` y con el código de país, tal como lo manda Meta (revisa el campo `"from"` de un mensaje de prueba si tienes dudas del formato exacto).

## 7. Probar

Escríbele al número de prueba desde tu celular (el que agregaste como destinatario verificado). Ejemplos que ya entiende:

- "¿Cuántas llaves KW1 quedan?"
- "Registra una venta de 2 llaves Cantol y 1 llavero"
- "Agenda un cambio de chapa mañana a las 9am para [nombre de un cliente ya registrado]"
- "Muéstrame los trabajos pendientes"

## Limitaciones de esta primera versión

- Solo mensajes de **texto** (no audio, no imágenes)
- El token de Meta de prueba **expira cada 24 horas** — para algo permanente, hay que generar un token de System User desde Business Manager (un paso más de configuración en Meta, no en el código)
- Cubre 4 acciones: consultar stock, registrar venta, agendar servicio a domicilio, ver trabajos pendientes. El patrón para agregar más (crear cliente, registrar compra, consultar ventas) es el mismo — se agrega la herramienta en `Herramientas.java` y su caso en `ComandoWhatsAppService.java`.
- Si el mensaje es ambiguo (dos productos que calzan con el nombre, un cliente que no existe), el sistema pregunta en vez de adivinar — a propósito, para no registrar algo mal por una mala interpretación.
