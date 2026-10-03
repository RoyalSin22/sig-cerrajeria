package com.cerrajeria.sig.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Envia mensajes salientes usando la WhatsApp Business Cloud API de Meta.
 * Doc: https://developers.facebook.com/docs/whatsapp/cloud-api/reference/messages
 */
@Component
public class WhatsAppClient {

    private final RestClient restClient;
    private final String phoneNumberId;

    public WhatsAppClient(
            @Value("${sig.whatsapp.access-token}") String accessToken,
            @Value("${sig.whatsapp.phone-number-id}") String phoneNumberId,
            @Value("${sig.whatsapp.api-version:v21.0}") String apiVersion
    ) {
        this.phoneNumberId = phoneNumberId;
        this.restClient = RestClient.builder()
            .baseUrl("https://graph.facebook.com/" + apiVersion)
            .defaultHeader("Authorization", "Bearer " + accessToken)
            .build();
    }

    /** Envia un mensaje de texto simple al numero indicado (formato E.164, sin el '+'). */
    public void enviarTexto(String telefonoDestino, String texto) {
        var body = Map.of(
            "messaging_product", "whatsapp",
            "to", telefonoDestino,
            "type", "text",
            "text", Map.of("body", texto)
        );

        restClient.post()
            .uri("/{phoneNumberId}/messages", phoneNumberId)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    }
}
