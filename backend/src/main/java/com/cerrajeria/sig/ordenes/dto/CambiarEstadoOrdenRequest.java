package com.cerrajeria.sig.ordenes.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoOrdenRequest(
    @NotBlank(message = "El nuevo estado es obligatorio") String nuevoEstado
) {
}
