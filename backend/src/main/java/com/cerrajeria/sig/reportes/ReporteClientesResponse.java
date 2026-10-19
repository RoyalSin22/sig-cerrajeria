package com.cerrajeria.sig.reportes;

import java.math.BigDecimal;
import java.util.List;

/**
 * RF-44: lo que dejo cada cliente en un rango, sumando ventas, taller (neto de
 * devoluciones) y servicios a domicilio, de mayor a menor.
 *
 * @param clientesConMovimiento cuantos clientes tuvieron algun movimiento (el ranking puede venir recortado)
 * @param ventasSinCliente      ventas de mostrador sin cliente asociado, que no se pueden atribuir a nadie
 */
public record ReporteClientesResponse(
    int clientesConMovimiento,
    BigDecimal ventasSinCliente,
    List<Cliente> ranking
) {
    public record Cliente(
        Long clienteId,
        String nombre,
        Movimiento ventas,
        Movimiento taller,
        Movimiento domicilio,
        BigDecimal total
    ) {
    }

    /** {@code cantidad}: ventas, ordenes con pagos o servicios realizados. */
    public record Movimiento(long cantidad, BigDecimal total) {
        static final Movimiento NINGUNO = new Movimiento(0, BigDecimal.ZERO);
    }
}
