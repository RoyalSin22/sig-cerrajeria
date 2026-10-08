package com.cerrajeria.sig.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;

/**
 * Seguridad stateless con JWT (RNF-02). El claim "role" del token (ej.
 * ROLE_ADMIN, ROLE_EMPLOYEE) se traduce directamente a una GrantedAuthority
 * de Spring Security, habilitando @PreAuthorize("hasRole('ADMIN')") en los
 * controladores de las fases siguientes.
 *
 * Nota sobre CSRF: se deshabilita porque toda la API usa autenticacion por
 * Bearer token (no cookies de sesion). El unico endpoint que usa una cookie
 * (el refresh token, httpOnly + SameSite=Strict segun Fase 5) queda mitigado
 * por SameSite=Strict; para un sistema interno de 2 usuarios esto es un
 * balance aceptable, documentado explicitamente aqui.
 *
 * CORS: se habilita explicitamente para el origen del frontend (Fase 9),
 * con allowCredentials=true porque el refresh token viaja en cookie. Ademas se
 * aceptan los origenes del acceso desde el celular (por defecto la direccion
 * HTTPS que da Tailscale, https://&lt;pc&gt;.&lt;red&gt;.ts.net; ver INSTALACION.md).
 * Detras de nginx el backend ve el pedido como de otro origen (otro host y
 * puerto), asi que un origen que no este en esta lista no puede ni iniciar sesion.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${sig.frontend-origin:http://localhost:5173}")
    private String frontendOrigin;

    @Value("${sig.origenes-celular:}")
    private String origenesCelular;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                    "/uploads/**",
                    "/api/v1/whatsapp/webhook"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Patrones (no origenes exactos) para admitir "https://*.ts.net"
        config.setAllowedOriginPatterns(java.util.stream.Stream.of(frontendOrigin, origenesCelular)
            .flatMap(lista -> java.util.Arrays.stream(lista.split(",")))
            .map(String::trim)
            .filter(origen -> !origen.isEmpty())
            .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true); // necesario para que el navegador envie la cookie refresh_token

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                return List.of();
            }
            // El rol ya viene como "ROLE_ADMIN" / "ROLE_EMPLOYEE" desde la BD (ver Fase 6)
            Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
            return authorities;
        });
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new org.springframework.security.authentication.ProviderManager(provider);
    }
}
