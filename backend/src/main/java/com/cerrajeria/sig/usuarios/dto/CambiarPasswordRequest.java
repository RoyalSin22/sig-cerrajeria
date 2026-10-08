package com.cerrajeria.sig.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
    @NotBlank(message = "La contraseña actual es obligatoria") String passwordActual,
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String passwordNueva
) {
}
