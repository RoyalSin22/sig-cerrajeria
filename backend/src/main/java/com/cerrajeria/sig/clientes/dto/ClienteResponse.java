package com.cerrajeria.sig.clientes.dto;

public record ClienteResponse(
    Long id,
    String nombre,
    String telefono,
    String direccion,
    String observaciones
) {
}
