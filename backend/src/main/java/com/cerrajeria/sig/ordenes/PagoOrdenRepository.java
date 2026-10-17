package com.cerrajeria.sig.ordenes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoOrdenRepository extends JpaRepository<PagoOrden, Long> {
    List<PagoOrden> findByOrdenTrabajoIdOrderByFechaHoraDesc(Long ordenTrabajoId);

    /** Ingresos netos del taller en el rango: adelantos y liquidaciones menos devoluciones. */
    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(SUM(CASE WHEN p.tipoPago = com.cerrajeria.sig.ordenes.TipoPagoOrden.DEVOLUCION
                                 THEN -p.monto ELSE p.monto END), 0)
        FROM PagoOrden p WHERE p.fechaHora BETWEEN :desde AND :hasta
        """)
    java.math.BigDecimal ingresosNetosEntre(
        @org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde,
        @org.springframework.data.repository.query.Param("hasta") java.time.LocalDateTime hasta);
}
