package com.cerrajeria.sig.ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RegistrarPagoOrdenRequest(
    @NotNull @Positive(message = "El monto debe ser mayor a cero") BigDecimal monto,
    @NotBlank(message = "El método de pago es obligatorio") String metodoPago
) {
}
