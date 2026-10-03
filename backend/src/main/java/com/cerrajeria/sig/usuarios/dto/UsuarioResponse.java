package com.cerrajeria.sig.usuarios.dto;

public record UsuarioResponse(
    Long id,
    String username,
    String nombre,
    String role,
    Boolean activo,
    String telefonoWhatsapp
) {
}
