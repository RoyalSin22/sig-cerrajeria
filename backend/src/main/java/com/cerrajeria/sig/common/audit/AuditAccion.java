package com.cerrajeria.sig.common.audit;

/** Coincide exactamente con el CHECK de audit_log.accion (Fase 6). */
public enum AuditAccion {
    CREAR,
    EDITAR,
    ELIMINAR,
    CAMBIO_ESTADO,
    AJUSTE_INVENTARIO,
    ANULACION,
    APERTURA_CAJA,
    CIERRE_CAJA,
    CAMBIO_PASSWORD
}
