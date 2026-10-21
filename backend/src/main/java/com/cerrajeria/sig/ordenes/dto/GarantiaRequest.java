package com.cerrajeria.sig.ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El cliente vuelve con una orden ya entregada: que fallo. */
public record GarantiaRequest(
    @NotBlank(message = "Describe qué falló")
    @Size(max = 1000, message = "La descripción admite hasta 1000 caracteres")
    String problema,
    String observaciones
) {
}
