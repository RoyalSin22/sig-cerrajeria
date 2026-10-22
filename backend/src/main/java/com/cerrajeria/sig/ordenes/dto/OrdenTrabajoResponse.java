package com.cerrajeria.sig.ordenes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrdenTrabajoResponse(
    Long id,
    String numeroOrden,
    String cliente,
    String productoRecibido,
    String problema,
    String diagnostico,
    BigDecimal precioTotal,
    BigDecimal montoPagado,
    BigDecimal saldoPendiente,
    String estado,
    LocalDateTime fechaIngreso,
    LocalDateTime fechaPrometida,
    LocalDateTime fechaEntrega,
    String observaciones,
    String motivoCancelacion,
    boolean garantia,
    /** Numero de la orden original si es una garantia (ej. OT-000012). */
    String numeroOrdenOrigen,
    /** Para avisarle por WhatsApp (comprobante, trabajo listo). */
    String telefonoCliente
) {
}
