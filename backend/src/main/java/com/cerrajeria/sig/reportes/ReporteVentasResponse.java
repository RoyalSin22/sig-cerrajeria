package com.cerrajeria.sig.reportes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reporte de ingresos del rango. {@code totalVentas} son solo las ventas de
 * mostrador; {@code ingresosTaller} (adelantos y liquidaciones menos
 * devoluciones) y {@code ingresosDomicilio} completan lo que gano el negocio
 * en {@code ingresosTotales}.
 */
public record ReporteVentasResponse(
    BigDecimal totalVentas,
    Long cantidadTransacciones,
    List<VentaPorMetodoPago> porMetodoPago,
    BigDecimal ingresosTaller,
    BigDecimal ingresosDomicilio,
    Long serviciosDomicilioRealizados,
    BigDecimal ingresosTotales
) {
    public record VentaPorMetodoPago(String metodoPago, BigDecimal total) {
    }
}
