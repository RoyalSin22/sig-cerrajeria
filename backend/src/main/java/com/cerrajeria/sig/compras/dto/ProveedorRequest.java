package com.cerrajeria.sig.compras.dto;

import jakarta.validation.constraints.NotBlank;

public record ProveedorRequest(
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    String telefono,
    String direccion
) {
}
