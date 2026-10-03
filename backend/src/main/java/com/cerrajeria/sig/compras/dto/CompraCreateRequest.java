package com.cerrajeria.sig.compras.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CompraCreateRequest(
    @NotNull(message = "El proveedor es obligatorio") Long proveedorId,
    @NotNull(message = "El producto es obligatorio") Long productoId,
    @NotNull @Positive(message = "La cantidad debe ser mayor a cero") Integer cantidad,
    @NotNull @Positive(message = "El precio unitario debe ser mayor a cero") BigDecimal precioUnitario,
    /** Si es true, exige caja abierta y descuenta el egreso de la caja activa (RF-29). */
    boolean pagarDeCaja
) {
}
