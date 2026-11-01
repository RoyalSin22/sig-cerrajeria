package com.cerrajeria.sig.asistente;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.function.Consumer;

/**
 * Catalogo de acciones que el asistente de WhatsApp puede ejecutar. Cubre
 * los ejemplos del alcance original del proyecto (Fase 1): consultar stock,
 * registrar una venta, agendar un servicio a domicilio, y ver los trabajos
 * pendientes. Se puede extender agregando una herramienta mas, siguiendo el
 * mismo patron, y su caso correspondiente en ComandoWhatsAppService.
 */
class Herramientas {

    static ArrayNode definiciones(ObjectMapper om) {
        ArrayNode tools = om.createArrayNode();

        tools.add(herramienta(om, "consultar_stock",
            "Consulta cuanto stock queda de un producto. Usar cuando preguntan '¿cuántas X quedan?' o 'stock de X'.",
            props -> props.putObject("producto")
                .put("type", "string")
                .put("description", "Nombre o palabra clave del producto, tal como lo escribio el usuario (ej. 'KW1', 'chapa yale')."),
            "producto"));

        tools.add(herramienta(om, "registrar_venta",
            "Registra una venta de mostrador con uno o mas productos. Usar cuando piden 'registra una venta de...' o 'vendí...'.",
            props -> {
                var items = props.putObject("items");
                items.put("type", "array");
                items.put("description", "Lista de productos vendidos, cada uno con su nombre y cantidad.");
                var itemSchema = items.putObject("items");
                itemSchema.put("type", "object");
                var itemProps = itemSchema.putObject("properties");
                itemProps.putObject("producto").put("type", "string").put("description", "Nombre del producto, tal como lo escribio el usuario.");
                itemProps.putObject("cantidad").put("type", "integer").put("description", "Cantidad vendida de ese producto.");
                itemProps.putObject("duplicado").put("type", "boolean").put("description",
                    "Solo llaves. true: duplicado o copia de llave (lo normal). false: solo si piden la llave virgen sola, "
                        + "sin duplicar (para otro cerrajero o de adorno). Si no lo dicen, omitirlo.");
                var itemReq = itemSchema.putArray("required");
                itemReq.add("producto");
                itemReq.add("cantidad");

                props.putObject("metodo_pago")
                    .put("type", "string")
                    .put("description", "EFECTIVO, YAPE, PLIN o TARJETA. Si no lo dicen, usar EFECTIVO.");
            },
            "items"));

        tools.add(herramienta(om, "agendar_servicio_domicilio",
            "Agenda un servicio a domicilio para un cliente YA REGISTRADO en el sistema. Usar cuando piden 'agenda...' con fecha y hora.",
            props -> {
                props.putObject("cliente").put("type", "string").put("description", "Nombre del cliente (debe existir ya en el sistema).");
                props.putObject("direccion").put("type", "string").put("description", "Direccion del servicio.");
                props.putObject("fecha").put("type", "string").put("description", "Fecha en formato AAAA-MM-DD. Si dicen 'mañana' o 'hoy', calcula la fecha real.");
                props.putObject("hora").put("type", "string").put("description", "Hora en formato HH:mm (24 horas).");
                props.putObject("tipo_servicio").put("type", "string").put("description", "Ej. 'Cambio de chapa', 'Apertura de puerta'.");
            },
            "cliente", "direccion", "fecha", "hora", "tipo_servicio"));

        tools.add(herramienta(om, "consultar_trabajos_pendientes",
            "Muestra un resumen de las ordenes de trabajo que aun no estan entregadas. Usar cuando piden 'trabajos pendientes' o similar.",
            props -> { /* sin parametros */ }));

        return tools;
    }

    private static ObjectNode herramienta(
            ObjectMapper om, String nombre, String descripcion,
            Consumer<ObjectNode> propiedades, String... requeridos
    ) {
        ObjectNode tool = om.createObjectNode();
        tool.put("name", nombre);
        tool.put("description", descripcion);

        ObjectNode schema = tool.putObject("input_schema");
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        propiedades.accept(props);

        if (requeridos.length > 0) {
            var req = schema.putArray("required");
            for (String r : requeridos) req.add(r);
        }
        return tool;
    }
}
