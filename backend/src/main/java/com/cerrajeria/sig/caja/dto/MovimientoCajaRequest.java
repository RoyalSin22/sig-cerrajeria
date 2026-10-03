package com.cerrajeria.sig.caja.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MovimientoCajaRequest(
    @NotBlank(message = "El tipo es obligatorio (INGRESO o EGRESO)") String tipo,
    @NotNull @Positive(message = "El monto debe ser mayor a cero") BigDecimal monto,
    @NotBlank(message = "El motivo es obligatorio") String motivo
) {
}
