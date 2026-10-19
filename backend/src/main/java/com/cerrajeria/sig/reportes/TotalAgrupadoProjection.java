package com.cerrajeria.sig.reportes;

import java.math.BigDecimal;

/**
 * Fila de un reporte agrupado (por cliente, por tipo de servicio, por tecnico):
 * {@code clave} identifica el grupo, {@code nombre} es lo que se muestra.
 */
public interface TotalAgrupadoProjection {
    Long getClave();
    String getNombre();
    Long getCantidad();
    BigDecimal getTotal();
}
