package com.cerrajeria.sig.ordenes;

public enum TipoPagoOrden {
    ADELANTO,
    LIQUIDACION,
    /** Devolucion del adelanto al cancelar la orden: egreso de caja (V9). */
    DEVOLUCION
}
