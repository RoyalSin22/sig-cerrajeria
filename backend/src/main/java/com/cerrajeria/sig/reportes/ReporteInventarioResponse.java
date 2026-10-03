package com.cerrajeria.sig.reportes;

import java.math.BigDecimal;
import java.util.List;

public record ReporteInventarioResponse(
    Integer totalProductosActivos,
    Integer totalUnidades,
    BigDecimal valorTotalInventario,
    List<ItemInventario> items
) {
    public record ItemInventario(
        String codigo, String nombre, String tipoProducto,
        Integer cantidadStock, BigDecimal precioVenta, BigDecimal valorLinea, Boolean stockBajo
    ) {
    }
}
