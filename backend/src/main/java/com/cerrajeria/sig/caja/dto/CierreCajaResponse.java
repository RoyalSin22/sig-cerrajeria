package com.cerrajeria.sig.caja.dto;

import java.math.BigDecimal;

public record CierreCajaResponse(
    CajaDiariaResponse caja,
    BigDecimal montoContado,
    BigDecimal diferencia
) {
}
