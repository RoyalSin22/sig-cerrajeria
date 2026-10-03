package com.cerrajeria.sig.clientes.dto;

import jakarta.validation.constraints.NotBlank;

public record ClienteRequest(
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    @NotBlank(message = "El teléfono es obligatorio") String telefono,
    String direccion,
    String observaciones
) {
}
