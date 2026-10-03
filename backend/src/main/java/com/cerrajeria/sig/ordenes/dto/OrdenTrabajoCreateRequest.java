package com.cerrajeria.sig.ordenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrdenTrabajoCreateRequest(
    @NotNull(message = "El cliente es obligatorio") Long clienteId,
    @NotBlank(message = "El producto recibido es obligatorio") String productoRecibido,
    @NotBlank(message = "El problema reportado es obligatorio") String problema,
    String diagnostico,
    @NotNull @Positive(message = "El precio total debe ser mayor a cero") BigDecimal precioTotal,
    @PositiveOrZero(message = "El adelanto no puede ser negativo") BigDecimal adelanto,
    String metodoPagoAdelanto,
    LocalDateTime fechaPrometida,
    String observaciones
) {
}
