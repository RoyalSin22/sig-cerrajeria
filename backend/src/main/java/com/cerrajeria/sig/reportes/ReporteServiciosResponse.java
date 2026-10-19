package com.cerrajeria.sig.reportes;

import java.math.BigDecimal;
import java.util.List;

/**
 * RF-43: servicios realizados en un rango. Domicilio se cuenta por la fecha en
 * que se realizo y cobro; el taller, por la fecha de entrega.
 */
public record ReporteServiciosResponse(
    Domicilio domicilio,
    Taller taller
) {
    public record Domicilio(
        long realizados,
        long cancelados,
        BigDecimal cobrado,
        List<Grupo> porTipo,
        List<Grupo> porTecnico
    ) {
    }

    /** {@code abiertas} es la cantidad actual (no depende del rango). */
    public record Taller(
        long recibidas,
        long entregadas,
        long abiertas,
        BigDecimal ingresosNetos
    ) {
    }

    public record Grupo(String nombre, long cantidad, BigDecimal total) {
    }
}
