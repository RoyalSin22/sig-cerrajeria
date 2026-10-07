package com.cerrajeria.sig.auth.dto;

public record LoginResponse(
    String accessToken,
    String username,
    String nombre,
    String role
) {
}
