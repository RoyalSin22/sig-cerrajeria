package com.cerrajeria.sig.usuarios.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioRequest(
    @NotNull(message = "El estado es obligatorio") Boolean activo
) {
}
