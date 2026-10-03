package com.cerrajeria.sig.ventas.dto;

import java.math.BigDecimal;

public record DetalleVentaResponse(
    Long productoId,
    String nombreProducto,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal
) {
}
