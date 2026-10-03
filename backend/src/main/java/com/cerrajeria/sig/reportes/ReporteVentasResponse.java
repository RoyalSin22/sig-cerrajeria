package com.cerrajeria.sig.reportes;

import java.math.BigDecimal;
import java.util.List;

public record ReporteVentasResponse(
    BigDecimal totalVentas,
    Long cantidadTransacciones,
    List<VentaPorMetodoPago> porMetodoPago
) {
    public record VentaPorMetodoPago(String metodoPago, BigDecimal total) {
    }
}
