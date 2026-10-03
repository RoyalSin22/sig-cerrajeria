package com.cerrajeria.sig.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * cantidad puede ser positiva (incremento) o negativa (decremento);
 * el motivo es obligatorio (Fase 6, HU-07).
 */
public record AjusteStockRequest(
    @NotNull(message = "La cantidad es obligatoria") Integer cantidad,
    @NotBlank(message = "El motivo del ajuste es obligatorio") String motivo
) {
}
