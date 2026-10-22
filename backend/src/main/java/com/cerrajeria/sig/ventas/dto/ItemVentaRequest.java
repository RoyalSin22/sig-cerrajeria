package com.cerrajeria.sig.ventas.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Una linea de la venta: un producto del inventario, o un servicio rapido sin
 * producto (abrir un candado, afilar una cuchilla) con su descripcion y precio.
 */
public record ItemVentaRequest(
    Long productoId,
    @NotNull @Positive(message = "La cantidad debe ser mayor a cero") Integer cantidad,
    /**
     * true: se cobra el precio de duplicado de la llave (material + corte), no
     * el de la llave sola. Boolean y no boolean: Jackson 3 rechaza un primitivo
     * ausente, y los clientes que no lo mandan (asistente, ventas normales) son
     * ventas de la llave sola.
     */
    Boolean duplicado,
    /** Solo en un servicio rapido: que se hizo (ej. "Abrir candado"). */
    @Size(max = 150, message = "La descripción admite hasta 150 caracteres") String descripcion,
    /** Solo en un servicio rapido: cuanto se cobra por unidad. */
    @Positive(message = "El precio del servicio debe ser mayor a cero") BigDecimal precio
) {

    public ItemVentaRequest(Long productoId, Integer cantidad) {
        this(productoId, cantidad, false, null, null);
    }

    public ItemVentaRequest(Long productoId, Integer cantidad, Boolean duplicado) {
        this(productoId, cantidad, duplicado, null, null);
    }

    public boolean esDuplicado() {
        return Boolean.TRUE.equals(duplicado);
    }

    public boolean esServicio() {
        return productoId == null;
    }

    @AssertTrue(message = "Cada línea lleva un producto, o un servicio con su descripción y precio")
    public boolean isLineaValida() {
        return productoId != null || (descripcion != null && !descripcion.isBlank() && precio != null);
    }
}
