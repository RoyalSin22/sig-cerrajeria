package com.cerrajeria.sig.auth.dto;

public record LoginResponse(
    String accessToken,
    String nombre,
    String role
) {
}
