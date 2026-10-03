package com.cerrajeria.sig.asistente.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AsistenteMensajeRequest(
    @NotBlank(message = "El mensaje no puede estar vacío") String mensaje,
    /** Turnos previos de esta conversacion (para que el asistente tenga contexto). Puede venir vacia. */
    List<TurnoChat> historial
) {
}
