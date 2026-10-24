package com.cerrajeria.sig.negocio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Lo mismo para leer y para guardar: nombre, telefono (solo digitos) y direccion. */
public record NegocioDto(
    @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "\\+?[0-9 ]{6,20}", message = "El teléfono solo lleva números (ej. 982616709)") String telefono,
    @NotBlank(message = "La dirección es obligatoria") @Size(max = 200) String direccion
) {
}
