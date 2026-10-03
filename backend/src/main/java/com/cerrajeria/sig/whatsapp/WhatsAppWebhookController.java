package com.cerrajeria.sig.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook de la WhatsApp Business Cloud API de Meta.
 * - GET: handshake de verificacion (una sola vez, al configurar el webhook en Meta).
 * - POST: mensajes entrantes.
 * Doc: https://developers.facebook.com/docs/graph-api/webhooks/getting-started
 */
@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
@RequiredArgsConstructor
@Tag(name = "WhatsApp")
public class WhatsAppWebhookController {

    private final ComandoWhatsAppService comandoWhatsAppService;

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
    public ResponseEntity<Void> recibir(@RequestBody JsonNode payload) {
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
        if (!"text".equals(primero.path("type").asText())) {
            return java.util.Optional.empty(); // por ahora solo se procesan mensajes de texto
        }

        String telefono = primero.path("from").asText();
        String texto = primero.path("text").path("body").asText();
        return java.util.Optional.of(new MensajeEntrante(telefono, texto));
    }

    private record MensajeEntrante(String telefono, String texto) {
    }
}
