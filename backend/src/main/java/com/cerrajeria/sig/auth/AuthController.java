package com.cerrajeria.sig.auth;

import com.cerrajeria.sig.auth.dto.LoginRequest;
import com.cerrajeria.sig.auth.dto.LoginResponse;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;
    private final JwtDecoder jwtDecoder;

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve un access token; el refresh token se envía en cookie httpOnly")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        Usuario usuario = usuarioRepository.findByUsername(request.username())
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return responseWithTokens(usuario);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Emite un nuevo access token a partir del refresh token de la cookie")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        if (refreshToken == null) {
            throw new BadCredentialsException("No hay sesión activa");
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(refreshToken);
        } catch (JwtException ex) {
            throw new BadCredentialsException("Sesión expirada, inicia sesión nuevamente");
        }

        if (!"refresh".equals(jwt.getClaimAsString("type"))) {
            throw new BadCredentialsException("Token inválido");
        }

        Usuario usuario = usuarioRepository.findByUsername(jwt.getSubject())
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return responseWithTokens(usuario);
    }

    @PostMapping("/logout")
    @Operation(summary = "Cierra sesión invalidando la cookie de refresh token (RF-03)")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(0) // expira inmediatamente
            .build();
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build();
    }

    private ResponseEntity<LoginResponse> responseWithTokens(Usuario usuario) {
        String accessToken = tokenService.generateAccessToken(usuario);
        String refreshToken = tokenService.generateRefreshToken(usuario);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(Duration.ofDays(tokenService.getRefreshTokenDays()))
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(new LoginResponse(accessToken, usuario.getNombre(), usuario.getRole().name()));
    }
}
