package com.cerrajeria.sig.ordenes;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoOrden {
    RECIBIDO,
    EN_REPARACION,
    ESPERANDO_REPUESTO,
    LISTO,
    ENTREGADO,
    /** El cliente se arrepintio o nunca la recogio (V9). Estado final, como ENTREGADO. */
    CANCELADO;

    /** Estados en los que la orden ya no se trabaja ni admite pagos. */
    public static final Set<EstadoOrden> FINALES = EnumSet.of(ENTREGADO, CANCELADO);

    public boolean esFinal() {
        return FINALES.contains(this);
    }
}
