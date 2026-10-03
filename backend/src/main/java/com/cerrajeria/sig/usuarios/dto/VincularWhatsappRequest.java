package com.cerrajeria.sig.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

/** El telefono debe venir en formato E.164 (ej. "+51987654321"), igual a como llega el webhook de Meta. */
public record VincularWhatsappRequest(
    @NotBlank(message = "El teléfono es obligatorio") String telefonoWhatsapp
) {
}
