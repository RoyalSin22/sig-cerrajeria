package com.cerrajeria.sig.reportes;

import java.math.BigDecimal;

public record ProductoMasVendidoResponse(
    Long productoId,
    String nombre,
    Long cantidadVendida,
    BigDecimal totalVendido
) {
}
