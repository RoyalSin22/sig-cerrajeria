package com.cerrajeria.sig.domicilio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicioDomicilioRepository extends JpaRepository<ServicioDomicilio, Long> {
    List<ServicioDomicilio> findByFechaBetweenOrderByFechaAscHoraAsc(LocalDate desde, LocalDate hasta);
    List<ServicioDomicilio> findByClienteIdOrderByFechaDesc(Long clienteId);

    /** Cobrado por servicios realizados en el rango (por la fecha en que se realizaron y cobraron). */
    @Query("""
        SELECT COALESCE(SUM(s.montoCobrado), 0) FROM ServicioDomicilio s
        WHERE s.estado = com.cerrajeria.sig.domicilio.EstadoServicio.REALIZADO
          AND s.fechaRealizado BETWEEN :desde AND :hasta
        """)
    BigDecimal cobradoEntre(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta);

    /** RF-43: servicios realizados en el rango, agrupados por tipo (la clave no aplica: 0). */
    @Query("""
        SELECT 0L AS clave, s.tipoServicio AS nombre, COUNT(s) AS cantidad,
               COALESCE(SUM(s.montoCobrado), 0) AS total
        FROM ServicioDomicilio s
        WHERE s.estado = com.cerrajeria.sig.domicilio.EstadoServicio.REALIZADO
          AND s.fechaRealizado BETWEEN :desde AND :hasta
        GROUP BY s.tipoServicio
        ORDER BY COUNT(s) DESC
        """)
    List<com.cerrajeria.sig.reportes.TotalAgrupadoProjection> realizadosPorTipo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** RF-43: servicios realizados en el rango, por quien los realizo y cobro. */
    @Query("""
        SELECT u.id AS clave, u.nombre AS nombre, COUNT(s) AS cantidad,
               COALESCE(SUM(s.montoCobrado), 0) AS total
        FROM ServicioDomicilio s JOIN s.realizadoPor u
        WHERE s.estado = com.cerrajeria.sig.domicilio.EstadoServicio.REALIZADO
          AND s.fechaRealizado BETWEEN :desde AND :hasta
        GROUP BY u.id, u.nombre
        ORDER BY COUNT(s) DESC
        """)
    List<com.cerrajeria.sig.reportes.TotalAgrupadoProjection> realizadosPorTecnico(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** RF-44: lo cobrado a cada cliente por servicios realizados en el rango. */
    @Query("""
        SELECT c.id AS clave, c.nombre AS nombre, COUNT(s) AS cantidad,
               COALESCE(SUM(s.montoCobrado), 0) AS total
        FROM ServicioDomicilio s JOIN s.cliente c
        WHERE s.estado = com.cerrajeria.sig.domicilio.EstadoServicio.REALIZADO
          AND s.fechaRealizado BETWEEN :desde AND :hasta
        GROUP BY c.id, c.nombre
        """)
    List<com.cerrajeria.sig.reportes.TotalAgrupadoProjection> cobradoPorCliente(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Cancelados entre los programados para esas fechas. */
    long countByEstadoAndFechaBetween(EstadoServicio estado, LocalDate desde, LocalDate hasta);

    long countByEstadoAndFechaRealizadoBetween(EstadoServicio estado, LocalDateTime desde,
                                               LocalDateTime hasta);
}
