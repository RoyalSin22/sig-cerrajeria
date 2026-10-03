package com.cerrajeria.sig.domicilio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record ServicioDomicilioRequest(
    @NotNull(message = "El cliente es obligatorio") Long clienteId,
    @NotBlank(message = "La dirección es obligatoria") String direccion,
    @NotBlank(message = "El teléfono es obligatorio") String telefono,
    @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
    @NotNull(message = "La hora es obligatoria") LocalTime hora,
    @NotBlank(message = "El tipo de servicio es obligatorio") String tipoServicio,
    String observaciones
) {
}
