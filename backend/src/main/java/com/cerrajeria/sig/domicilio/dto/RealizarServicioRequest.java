package com.cerrajeria.sig.domicilio.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Marca el servicio como REALIZADO con lo que se cobro. {@code monto} 0 es un
 * servicio sin cobro (garantia, cortesia) y no necesita metodo de pago.
 */
public record RealizarServicioRequest(
    @NotNull(message = "El monto cobrado es obligatorio (0 si no se cobró)")
    @PositiveOrZero(message = "El monto cobrado no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales")
    BigDecimal monto,
    String metodoPago
) {
}
