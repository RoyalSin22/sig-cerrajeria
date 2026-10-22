package com.cerrajeria.sig.ventas;

import com.cerrajeria.sig.inventario.Producto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** subtotal es columna GENERATED en la BD (Fase 6); no se mapea aqui, se calcula en el DTO. */
@Entity
@Table(name = "detalle_ventas")
@Getter
@Setter
@NoArgsConstructor
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    /** null en un servicio rapido (abrir un candado): ahi va la descripcion (V12). */
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(length = 150)
    private String descripcion;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "es_duplicado", nullable = false)
    private boolean duplicado;

    /** Lo que se muestra en el detalle: el producto, o la descripcion del servicio. */
    public String nombreLinea() {
        return producto != null ? producto.getNombre() : descripcion;
    }
}
