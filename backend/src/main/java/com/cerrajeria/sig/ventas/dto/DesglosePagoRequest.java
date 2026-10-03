package com.cerrajeria.sig.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DesglosePagoRequest(
    @NotBlank(message = "El método es obligatorio") String metodoPago,
    @NotNull @Positive(message = "El monto debe ser mayor a cero") BigDecimal monto
) {
}
