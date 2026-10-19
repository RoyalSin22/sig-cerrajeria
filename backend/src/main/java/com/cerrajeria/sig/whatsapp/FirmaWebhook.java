package com.cerrajeria.sig.whatsapp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifica la cabecera X-Hub-Signature-256 que Meta agrega a cada POST del
 * webhook: "sha256=" + HMAC-SHA256 del cuerpo crudo con el App Secret.
 */
final class FirmaWebhook {

    private static final String PREFIJO = "sha256=";

    private FirmaWebhook() {
    }

    static boolean esValida(byte[] cuerpo, String cabecera, String appSecret) {
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperada = mac.doFinal(cuerpo);
            byte[] recibida = HexFormat.of().parseHex(cabecera.substring(PREFIJO.length()));
            return MessageDigest.isEqual(esperada, recibida); // tiempo constante
        } catch (IllegalArgumentException ex) {
            return false; // no es hexadecimal
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA256 no disponible", ex);
        }
    }
}
