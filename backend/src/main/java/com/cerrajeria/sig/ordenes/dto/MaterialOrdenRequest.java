package com.cerrajeria.sig.ordenes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Material del inventario usado en la orden (sale del stock por el Kardex). */
public record MaterialOrdenRequest(
    @NotNull(message = "El producto es obligatorio") Long productoId,
    @NotNull @Positive(message = "La cantidad debe ser mayor a cero") Integer cantidad
) {
}
