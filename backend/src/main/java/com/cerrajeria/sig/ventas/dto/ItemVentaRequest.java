package com.cerrajeria.sig.ventas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemVentaRequest(
    @NotNull(message = "El producto es obligatorio") Long productoId,
    @NotNull @Positive(message = "La cantidad debe ser mayor a cero") Integer cantidad,
    /**
     * true: se cobra el precio de duplicado de la llave (material + corte), no
     * el de la llave sola. Boolean y no boolean: Jackson 3 rechaza un primitivo
     * ausente, y los clientes que no lo mandan (asistente, ventas normales) son
     * ventas de la llave sola.
     */
    Boolean duplicado
) {

    public ItemVentaRequest(Long productoId, Integer cantidad) {
        this(productoId, cantidad, false);
    }

    public boolean esDuplicado() {
        return Boolean.TRUE.equals(duplicado);
    }
}
