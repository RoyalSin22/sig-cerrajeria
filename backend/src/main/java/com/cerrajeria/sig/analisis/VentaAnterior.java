package com.cerrajeria.sig.analisis;

import com.cerrajeria.sig.inventario.Producto;
import com.cerrajeria.sig.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una venta de antes del sistema, cargada desde archivo para Power BI (V15).
 * No es una {@code Venta}: no tiene caja ni movio stock, a proposito.
 */
@Entity
@Table(name = "ventas_anteriores")
@Getter
@Setter
@NoArgsConstructor
public class VentaAnterior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 150)
    private String descripcion;

    /** Solo si lo escrito coincide con un producto del inventario. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    /** EFECTIVO, YAPE, PLIN o TARJETA; null si el archivo no lo dice. */
    @Column(name = "metodo_pago", length = 20)
    private String metodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
