package com.cerrajeria.sig.asistente.dto;

import tools.jackson.databind.JsonNode;

/** Resultado de pedirle a Claude que interprete un mensaje de WhatsApp. */
public record ComandoInterpretado(
    String herramienta,   // null si Claude no reconocio ninguna accion (respuesta de texto libre)
    JsonNode parametros,
    String textoLibre     // respuesta directa de Claude cuando no hay herramienta que aplique
) {
    public boolean tieneAccion() {
        return herramienta != null;
    }
}
