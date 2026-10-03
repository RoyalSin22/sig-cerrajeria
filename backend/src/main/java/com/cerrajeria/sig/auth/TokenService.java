package com.cerrajeria.sig.auth;

import com.cerrajeria.sig.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Emite access tokens (vida corta, en memoria en el frontend) y refresh
 * tokens (vida larga, en cookie httpOnly) segun la decision de Fase 5.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String ISSUER = "sig-cerrajeria";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtEncoder jwtEncoder;

    @Value("${sig.jwt.access-token-minutes:15}")
    private long accessTokenMinutes;

    @Value("${sig.jwt.refresh-token-days:7}")
    private long refreshTokenDays;

    public String generateAccessToken(Usuario usuario) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenMinutes, ChronoUnit.MINUTES))
            .subject(usuario.getUsername())
            .claim(CLAIM_ROLE, usuario.getRole().name())
            .claim("nombre", usuario.getNombre())
            .build();
        return encode(claims);
    }

    public String generateRefreshToken(Usuario usuario) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .issuedAt(now)
            .expiresAt(now.plus(refreshTokenDays, ChronoUnit.DAYS))
            .subject(usuario.getUsername())
            .claim(CLAIM_TYPE, TYPE_REFRESH)
            .build();
        return encode(claims);
    }

    public long getRefreshTokenDays() {
        return refreshTokenDays;
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}

