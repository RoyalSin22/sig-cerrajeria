package com.cerrajeria.sig.whatsapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El webhook de WhatsApp es publico y el telefono remitente decide con que
 * cuenta actua el asistente: solo se aceptan mensajes firmados por Meta.
 */
@DisplayName("FirmaWebhook - firma X-Hub-Signature-256 de Meta")
class FirmaWebhookTest {

    private static final String SECRETO = "secreto-de-la-app";
    private static final byte[] CUERPO =
        "{\"entry\":[{\"changes\":[{\"value\":{\"messages\":[]}}]}]}".getBytes(StandardCharsets.UTF_8);

    private static String firmar(byte[] cuerpo, String secreto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(cuerpo));
    }

    @Test
    @DisplayName("Acepta el cuerpo firmado con el App Secret")
    void aceptaFirmaCorrecta() throws Exception {
        assertThat(FirmaWebhook.esValida(CUERPO, firmar(CUERPO, SECRETO), SECRETO)).isTrue();
    }

    @Test
    @DisplayName("Rechaza sin firma, con otro secreto, con el cuerpo alterado o con basura")
    void rechazaFirmasInvalidas() throws Exception {
        assertThat(FirmaWebhook.esValida(CUERPO, null, SECRETO)).isFalse();
        assertThat(FirmaWebhook.esValida(CUERPO, firmar(CUERPO, "otro"), SECRETO)).isFalse();
        byte[] alterado = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        assertThat(FirmaWebhook.esValida(alterado, firmar(CUERPO, SECRETO), SECRETO)).isFalse();
        assertThat(FirmaWebhook.esValida(CUERPO, "sha256=no-es-hex", SECRETO)).isFalse();
        assertThat(FirmaWebhook.esValida(CUERPO, firmar(CUERPO, SECRETO).substring(7), SECRETO))
            .as("sin el prefijo sha256=").isFalse();
    }
}
