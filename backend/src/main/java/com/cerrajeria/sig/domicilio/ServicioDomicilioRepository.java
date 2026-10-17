package com.cerrajeria.sig.domicilio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ServicioDomicilioRepository extends JpaRepository<ServicioDomicilio, Long> {
    List<ServicioDomicilio> findByFechaBetweenOrderByFechaAscHoraAsc(LocalDate desde, LocalDate hasta);
    List<ServicioDomicilio> findByClienteIdOrderByFechaDesc(Long clienteId);

    /** Cobrado por servicios realizados en el rango (por la fecha en que se realizaron y cobraron). */
    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(SUM(s.montoCobrado), 0) FROM ServicioDomicilio s
        WHERE s.estado = com.cerrajeria.sig.domicilio.EstadoServicio.REALIZADO
          AND s.fechaRealizado BETWEEN :desde AND :hasta
        """)
    java.math.BigDecimal cobradoEntre(
        @org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde,
        @org.springframework.data.repository.query.Param("hasta") java.time.LocalDateTime hasta);

    long countByEstadoAndFechaRealizadoBetween(EstadoServicio estado, java.time.LocalDateTime desde,
                                               java.time.LocalDateTime hasta);
}
