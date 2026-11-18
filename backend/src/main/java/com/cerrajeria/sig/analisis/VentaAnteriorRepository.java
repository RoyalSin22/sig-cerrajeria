package com.cerrajeria.sig.analisis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface VentaAnteriorRepository extends JpaRepository<VentaAnterior, Long> {

    @Query("""
        SELECT COUNT(v) AS filas, COALESCE(SUM(v.cantidad * v.precioUnitario), 0) AS total,
               MIN(v.fecha) AS desde, MAX(v.fecha) AS hasta
        FROM VentaAnterior v
        """)
    Resumen resumen();

    interface Resumen {
        long getFilas();
        BigDecimal getTotal();
        LocalDate getDesde();
        LocalDate getHasta();
    }
}
