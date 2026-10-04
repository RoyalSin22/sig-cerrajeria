package com.cerrajeria.sig.caja.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CajaDiariaResponse(
    Long id,
    String usuario,
    LocalDateTime fechaApertura,
    LocalDateTime fechaCierre,
    BigDecimal montoApertura,
    BigDecimal montoCierre,
    BigDecimal montoIngresos,
    BigDecimal montoEgresos,
    BigDecimal montoYape,
    BigDecimal montoPlin,
    BigDecimal montoTarjeta,
    BigDecimal saldoEsperado,
    BigDecimal efectivoEsperado,
    String estado
) {
}
