package com.cerrajeria.sig.compras.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Todo lo que se trajo de un distribuidor en una sola vuelta. Cada item queda
 * como una compra propia (con su movimiento de Kardex), pero se registran
 * juntas: si una falla, no se guarda ninguna.
 */
public record CompraLoteRequest(
    @NotNull(message = "El proveedor es obligatorio") Long proveedorId,
    @NotEmpty(message = "Agrega al menos un producto")
    @Size(max = 200, message = "Máximo 200 productos por compra") List<@Valid Item> items,
    /** Si es true, exige caja abierta y descuenta el total de la caja activa (RF-29). */
    boolean pagarDeCaja
) {

    public record Item(
        @NotNull(message = "El producto es obligatorio") Long productoId,
        @NotNull @Positive(message = "La cantidad debe ser mayor a cero") Integer cantidad,
        @NotNull @Positive(message = "El precio unitario debe ser mayor a cero") BigDecimal precioUnitario
    ) {
    }
}
