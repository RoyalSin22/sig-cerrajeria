package com.cerrajeria.sig.ventas.dto;

import java.math.BigDecimal;

public record DesglosePagoResponse(String metodoPago, BigDecimal monto) {
}
