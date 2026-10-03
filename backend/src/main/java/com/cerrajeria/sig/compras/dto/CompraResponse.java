package com.cerrajeria.sig.compras.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraResponse(
    Long id,
    String proveedor,
    String producto,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal precioTotal,
    LocalDate fecha
) {
}
