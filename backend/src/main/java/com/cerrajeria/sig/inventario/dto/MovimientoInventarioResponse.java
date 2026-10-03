package com.cerrajeria.sig.inventario.dto;

import java.time.LocalDateTime;

public record MovimientoInventarioResponse(
    Long id,
    String tipoMovimiento,
    Integer cantidad,
    Integer saldoAnterior,
    Integer saldoNuevo,
    String referenciaTipo,
    Long referenciaId,
    String motivo,
    String usuario,
    LocalDateTime fechaHora
) {
}
