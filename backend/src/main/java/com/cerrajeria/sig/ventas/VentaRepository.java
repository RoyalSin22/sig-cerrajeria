package com.cerrajeria.sig.ventas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByClienteIdOrderByFechaHoraDesc(Long clienteId);

    Page<Venta> findAllByOrderByFechaHoraDesc(Pageable pageable);

    @Query("SELECT v FROM Venta v WHERE v.fechaHora BETWEEN :desde AND :hasta ORDER BY v.fechaHora DESC")
    List<Venta> findEntreFechas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // Los agregados de reportes excluyen ventas anuladas a proposito: no deben
    // contarse como ingresos reales del negocio.
    @Query("SELECT COALESCE(SUM(v.montoTotal), 0) FROM Venta v WHERE v.anulada = false AND v.fechaHora BETWEEN :desde AND :hasta")
    BigDecimal totalEntreFechas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.anulada = false AND v.fechaHora BETWEEN :desde AND :hasta")
    Long contarEntreFechas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
        SELECT v.metodoPago AS metodoPago, COALESCE(SUM(v.montoTotal), 0) AS total
        FROM Venta v WHERE v.anulada = false AND v.fechaHora BETWEEN :desde AND :hasta
        GROUP BY v.metodoPago
        """)
    List<VentaPorMetodoPagoProjection> totalPorMetodoPago(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    interface VentaPorMetodoPagoProjection {
        com.cerrajeria.sig.common.MetodoPago getMetodoPago();
        BigDecimal getTotal();
    }
}
