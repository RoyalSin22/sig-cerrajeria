package com.cerrajeria.sig.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCreateRequest(
    @NotBlank(message = "El usuario es obligatorio") String username,
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String password,
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    @NotBlank(message = "El rol es obligatorio (ROLE_ADMIN o ROLE_EMPLOYEE)") String role
) {
}
