package com.cerrajeria.sig.domicilio.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoServicioRequest(
    @NotBlank(message = "El nuevo estado es obligatorio") String nuevoEstado
) {
}
