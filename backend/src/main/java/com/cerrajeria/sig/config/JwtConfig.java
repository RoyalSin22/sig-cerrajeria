package com.cerrajeria.sig.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Emision y validacion de JWT usando las clases nativas de Spring Security
 * (NimbusJwtEncoder / NimbusJwtDecoder), en lugar de una libreria externa
 * como jjwt. Decision tomada en Fase 5 (JWT access en memoria + refresh en
 * cookie httpOnly) y ajustada aqui para Spring Boot 4 / Jackson 3, donde la
 * compatibilidad de jjwt con Jackson 3 aun no esta garantizada, mientras que
 * NimbusJwtEncoder/Decoder son parte de spring-security-oauth2-jose, ya
 * probado contra Spring Boot 4.
 */
@Configuration
public class JwtConfig {

    @Value("${sig.jwt.secret}")
    private String secret;

    @Bean
    public JwtEncoder jwtEncoder() {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }
}
