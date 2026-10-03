package com.cerrajeria.sig.ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(
    Long id,
    String cliente,
    String usuario,
    LocalDateTime fechaHora,
    BigDecimal montoTotal,
    String metodoPago,
    List<DetalleVentaResponse> detalles,
    Boolean anulada,
    List<DesglosePagoResponse> desglosePago
) {
}
