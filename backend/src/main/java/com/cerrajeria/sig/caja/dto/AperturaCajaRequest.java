package com.cerrajeria.sig.caja.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AperturaCajaRequest(
    @NotNull(message = "El monto de apertura es obligatorio")
    @PositiveOrZero(message = "El monto de apertura no puede ser negativo")
    BigDecimal montoApertura
) {
}
