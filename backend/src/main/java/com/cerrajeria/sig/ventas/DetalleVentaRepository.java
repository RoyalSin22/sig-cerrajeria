package com.cerrajeria.sig.ventas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    @Query("""
        SELECT dv.producto.id AS productoId, dv.producto.nombre AS nombre,
               SUM(dv.cantidad) AS cantidadVendida,
               SUM(dv.cantidad * dv.precioUnitario) AS totalVendido
        FROM DetalleVenta dv
        WHERE dv.venta.anulada = false AND dv.venta.fechaHora BETWEEN :desde AND :hasta
        GROUP BY dv.producto.id, dv.producto.nombre
        ORDER BY SUM(dv.cantidad) DESC
        """)
    List<ProductoMasVendidoProjection> productosMasVendidos(
        @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    interface ProductoMasVendidoProjection {
        Long getProductoId();
        String getNombre();
        Long getCantidadVendida();
        BigDecimal getTotalVendido();
    }
}
