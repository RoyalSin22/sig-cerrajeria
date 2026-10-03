package com.cerrajeria.sig.caja.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoCajaResponse(
    Long id,
    String tipo,
    BigDecimal monto,
    String motivo,
    String usuario,
    LocalDateTime fechaHora
) {
}
