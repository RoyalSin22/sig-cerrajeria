package com.cerrajeria.sig.ventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record VentaCreateRequest(
    Long clienteId,
    @NotBlank(message = "El método de pago es obligatorio") String metodoPago,
    @NotEmpty(message = "La venta debe tener al menos un producto")
    List<@Valid ItemVentaRequest> items,
    /** Obligatorio y debe sumar el total cuando metodoPago = MIXTO; ignorado en otro caso. */
    List<@Valid DesglosePagoRequest> desglosePago
) {
}
