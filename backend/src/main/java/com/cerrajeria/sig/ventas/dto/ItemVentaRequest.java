package com.cerrajeria.sig.ventas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemVentaRequest(
    @NotNull(message = "El producto es obligatorio") Long productoId,
    @NotNull @Positive(message = "La cantidad debe ser mayor a cero") Integer cantidad
) {
}
