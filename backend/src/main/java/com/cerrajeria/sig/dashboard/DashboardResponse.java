package com.cerrajeria.sig.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
    BigDecimal ventasHoy,
    BigDecimal ventasMes,
    Long trabajosPendientes,
    Long trabajosListos,
    Long serviciosDomicilioProximos,
    Long productosStockBajo,
    List<String> alertasStockBajo,
    List<MovimientoReciente> ultimosMovimientos
) {
    public record MovimientoReciente(
        String descripcion,
        String usuario,
        LocalDateTime fechaHora
    ) {
    }
}
