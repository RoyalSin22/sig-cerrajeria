package com.cerrajeria.sig.asistente;

import com.cerrajeria.sig.asistente.dto.ComandoInterpretado;
import com.cerrajeria.sig.asistente.dto.TurnoChat;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Traduce un mensaje en lenguaje natural (desde el chat de la app o desde
 * WhatsApp) a una accion concreta del sistema, usando la API de Claude con
 * "tool use": cada accion soportada se declara como una herramienta con su
 * esquema de parametros, y Claude decide cual aplica y con que datos.
 */
@Component
public class ClaudeClient {

    private static final int MAX_TURNOS_HISTORIAL = 6;

    private static final String SYSTEM_PROMPT = """
        Eres el asistente del sistema de una cerrajeria en Peru (SIG-Cerrajeria),
        usado por sus empleados y administradores (no por clientes finales).
        Interpretas sus mensajes y decides que accion del sistema corresponde,
        usando SIEMPRE una de las herramientas disponibles cuando el mensaje pide
        una accion o una consulta que ellas cubren.

        Si el mensaje es un saludo, una duda general sobre la tienda, o no
        corresponde a ninguna herramienta, respondes en texto plano, breve y en
        espanol, sin inventar datos del negocio que no te dieron.

        No inventes cantidades, nombres de producto ni fechas: si el mensaje es
        ambiguo (por ejemplo, no dice cuantas unidades), pide la aclaracion en
        texto plano en vez de adivinar. Usa el historial de la conversacion para
        entender referencias como "ese producto" o "la misma hora de ayer".
        """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public ClaudeClient(
            @Value("${sig.anthropic.api-key}") String apiKey,
            @Value("${sig.anthropic.model:claude-sonnet-5}") String model,
            ObjectMapper objectMapper
    ) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
            .baseUrl("https://api.anthropic.com/v1")
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", "2023-06-01")
            .defaultHeader("content-type", "application/json")
            .build();
    }

    public ComandoInterpretado interpretar(List<TurnoChat> historial, String mensajeNuevo) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        // Claude Sonnet 5 piensa (thinking adaptativo) aunque no se pida, y esos
        // tokens cuentan dentro de max_tokens: con 1024 podia cortarse antes de
        // llegar a la llamada a la herramienta. Interpretar un comando es una
        // tarea corta, asi que se pide esfuerzo bajo.
        body.put("max_tokens", 4096);
        body.putObject("output_config").put("effort", "low");
        body.put("system", SYSTEM_PROMPT);

        ArrayNode messages = body.putArray("messages");
        historial.stream()
            .skip(Math.max(0, historial.size() - MAX_TURNOS_HISTORIAL))
            .forEach(turno -> {
                ObjectNode m = messages.addObject();
                m.put("role", turno.rol());
                m.put("content", turno.texto());
            });

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", mensajeNuevo);

        body.set("tools", Herramientas.definiciones(objectMapper));

        JsonNode respuesta = restClient.post()
            .uri("/messages")
            .body(body)
            .retrieve()
            .body(JsonNode.class);

        return parsearRespuesta(respuesta);
    }

    private ComandoInterpretado parsearRespuesta(JsonNode respuesta) {
        String motivoFin = respuesta.path("stop_reason").asString();
        if ("refusal".equals(motivoFin)) {
            return new ComandoInterpretado(null, null, "No puedo ayudar con ese pedido.");
        }
        JsonNode content = respuesta.path("content");
        String textoLibre = null;

        for (JsonNode bloque : content) {
            String tipo = bloque.path("type").asString();
            if ("tool_use".equals(tipo)) {
                String nombreHerramienta = bloque.path("name").asString();
                JsonNode parametros = bloque.path("input");
                return new ComandoInterpretado(nombreHerramienta, parametros, null);
            }
            if ("text".equals(tipo)) {
                textoLibre = bloque.path("text").asString();
            }
        }

        return new ComandoInterpretado(null, null,
            textoLibre != null ? textoLibre : "No entendí el mensaje. ¿Puedes reformularlo?");
    }
}
