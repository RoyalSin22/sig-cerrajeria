package com.cerrajeria.sig.ordenes.dto;

import java.time.LocalDateTime;

public record MaterialOrdenResponse(
    Long productoId,
    String codigo,
    String nombre,
    int cantidad,
    String usuario,
    LocalDateTime fechaHora
) {
}
