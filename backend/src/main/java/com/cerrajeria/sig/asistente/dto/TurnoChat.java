package com.cerrajeria.sig.asistente.dto;

/** Un turno previo de la conversacion, para que Claude tenga contexto (rol: "user" o "assistant"). */
public record TurnoChat(String rol, String texto) {
}
