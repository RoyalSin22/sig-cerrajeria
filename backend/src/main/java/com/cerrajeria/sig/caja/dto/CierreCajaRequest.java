package com.cerrajeria.sig.caja.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CierreCajaRequest(
    @NotNull(message = "El monto contado es obligatorio")
    @PositiveOrZero(message = "El monto contado no puede ser negativo")
    BigDecimal montoContado
) {
}
