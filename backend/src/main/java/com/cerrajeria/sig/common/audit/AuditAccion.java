package com.cerrajeria.sig.common.audit;

/** Coincide exactamente con el CHECK de audit_log.accion (V1, V8 y V11). */
public enum AuditAccion {
    CREAR,
    EDITAR,
    ELIMINAR,
    CAMBIO_ESTADO,
    AJUSTE_INVENTARIO,
    ANULACION,
    APERTURA_CAJA,
    CIERRE_CAJA,
    CAMBIO_PASSWORD,
    /** Ingreso o egreso manual de caja (V11). */
    MOVIMIENTO_CAJA,
    /** Orden de garantia abierta sobre una orden entregada (V11). */
    GARANTIA
}
