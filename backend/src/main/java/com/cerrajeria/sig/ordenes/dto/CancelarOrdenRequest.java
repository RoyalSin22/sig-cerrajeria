package com.cerrajeria.sig.ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cancelar una orden. {@code metodoDevolucion} solo se usa si se devuelve el
 * adelanto y la orden tiene algo cobrado.
 */
public record CancelarOrdenRequest(
    @NotBlank(message = "El motivo de la cancelación es obligatorio")
    @Size(max = 500, message = "El motivo admite hasta 500 caracteres")
    String motivo,
    boolean devolverAdelanto,
    String metodoDevolucion
) {
}
