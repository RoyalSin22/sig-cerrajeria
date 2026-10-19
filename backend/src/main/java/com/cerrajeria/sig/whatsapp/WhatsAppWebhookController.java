package com.cerrajeria.sig.whatsapp;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook de la WhatsApp Business Cloud API de Meta.
 * - GET: handshake de verificacion (una sola vez, al configurar el webhook en Meta).
 * - POST: mensajes entrantes. Solo se procesan si traen la firma
 *   X-Hub-Signature-256 de Meta: el webhook es publico (ngrok) y el remitente
 *   decide la cuenta con la que actua el asistente, asi que sin firma
 *   cualquiera podria registrar ventas haciendose pasar por el telefono del admin.
 * Doc: https://developers.facebook.com/docs/graph-api/webhooks/getting-started
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
@RequiredArgsConstructor
@Tag(name = "WhatsApp")
public class WhatsAppWebhookController {

    private final ComandoWhatsAppService comandoWhatsAppService;
    private final ObjectMapper objectMapper;

    @Value("${sig.whatsapp.app-secret:}")
    private String appSecret;

    @Value("${sig.whatsapp.verify-token}")
    private String verifyToken;

    @GetMapping
    @Operation(summary = "Verificacion del webhook (la hace Meta una sola vez, al configurarlo)")
    public ResponseEntity<String> verificar(
            @RequestParam("hub.mode") String modo,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge
    ) {
        if ("subscribe".equals(modo) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).build();
    }

    @PostMapping
    @Operation(summary = "Recibe mensajes entrantes de WhatsApp")
    public ResponseEntity<Void> recibir(
            @RequestBody byte[] cuerpo,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String firma) {
        if (appSecret == null || appSecret.isBlank()) {
            log.error("Mensaje de WhatsApp rechazado: falta WHATSAPP_APP_SECRET para verificar la firma de Meta");
            return ResponseEntity.status(403).build();
        }
        if (!FirmaWebhook.esValida(cuerpo, firma, appSecret)) {
            log.warn("Mensaje de WhatsApp rechazado: firma X-Hub-Signature-256 ausente o invalida");
            return ResponseEntity.status(403).build();
        }

        JsonNode payload = objectMapper.readTree(cuerpo);
        extraerMensaje(payload).ifPresent(mensaje ->
            comandoWhatsAppService.procesarMensaje(mensaje.telefono(), mensaje.texto()));

        // Siempre 200: Meta reintenta agresivamente si no responde rapido con OK,
        // incluso para eventos que no nos interesan (confirmaciones de entrega, etc.)
        return ResponseEntity.ok().build();
    }

    private java.util.Optional<MensajeEntrante> extraerMensaje(JsonNode payload) {
        JsonNode mensajes = payload
            .path("entry").path(0)
            .path("changes").path(0)
            .path("value").path("messages");

        if (!mensajes.isArray() || mensajes.isEmpty()) {
            return java.util.Optional.empty(); // ej. una confirmacion de "leido", no un mensaje nuevo
        }

        JsonNode primero = mensajes.path(0);
        if (!"text".equals(primero.path("type").asString())) {
            return java.util.Optional.empty(); // por ahora solo se procesan mensajes de texto
        }

        String telefono = primero.path("from").asString();
        String texto = primero.path("text").path("body").asString();
        return java.util.Optional.of(new MensajeEntrante(telefono, texto));
    }

    private record MensajeEntrante(String telefono, String texto) {
    }
}
