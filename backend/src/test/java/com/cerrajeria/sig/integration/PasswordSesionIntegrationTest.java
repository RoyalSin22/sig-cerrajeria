package com.cerrajeria.sig.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cambio de contrasena y cierre de sesiones contra una PostgreSQL real (V8):
 * el refresh token dura dias y no se guarda en la base, asi que lo unico que
 * corta una sesion abierta es la verificacion de /auth/refresh.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Integración: cambio de contraseña y cierre de sesiones")
class PasswordSesionIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
        .withDatabaseName("sig_cerrajeria_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private record Sesion(String accessToken, Cookie refresh) {
        String bearer() { return "Bearer " + accessToken; }
    }

    private MvcResult intentarLogin(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
            .andReturn();
    }

    private Sesion login(String username, String password) throws Exception {
        MvcResult r = intentarLogin(username, password);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        String token = objectMapper.readTree(r.getResponse().getContentAsString()).get("accessToken").asString();
        return new Sesion(token, r.getResponse().getCookie("refresh_token"));
    }

    private int refrescar(Cookie refresh) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh)).andReturn().getResponse().getStatus();
    }

    /** El "iat" del JWT tiene precision de segundos: se espera para que el token quede claramente antes del cambio. */
    private static void pasarAlSiguienteSegundo() throws InterruptedException {
        Thread.sleep(1100);
    }

    @Test
    @DisplayName("Cambiar la contraseña invalida la sesión anterior y la contraseña vieja")
    void cambioDePassword_cierraSesionesYReemplazaLaClave() throws Exception {
        Sesion vieja = login("admin", "admin123");
        assertThat(refrescar(vieja.refresh())).isEqualTo(200);
        pasarAlSiguienteSegundo();

        // Contraseña actual incorrecta: 400 (no 401, que el frontend tomaria como sesion vencida)
        mockMvc.perform(patch("/api/v1/usuarios/me/password")
                .header("Authorization", vieja.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"adivinada\",\"passwordNueva\":\"NuevaClave2026\"}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/usuarios/me/password")
                .header("Authorization", vieja.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"admin123\",\"passwordNueva\":\"NuevaClave2026\"}"))
            .andExpect(status().isNoContent());

        assertThat(refrescar(vieja.refresh())).as("refresh token emitido antes del cambio").isEqualTo(401);
        assertThat(intentarLogin("admin", "admin123").getResponse().getStatus()).isEqualTo(401);

        Sesion nueva = login("admin", "NuevaClave2026");
        assertThat(refrescar(nueva.refresh())).as("sesion iniciada despues del cambio").isEqualTo(200);
    }

    @Test
    @DisplayName("Un empleado cambia su propia contraseña; si se lo desactiva pierde la sesión y no puede entrar")
    void empleado_cambiaSuPassword_yDesactivadoPierdeLaSesion() throws Exception {
        // Este test corre con su propio admin: el otro test puede haberle cambiado la contraseña
        String adminPassword = intentarLogin("admin", "admin123").getResponse().getStatus() == 200
            ? "admin123" : "NuevaClave2026";
        Sesion admin = login("admin", adminPassword);

        MvcResult creado = mockMvc.perform(post("/api/v1/usuarios")
                .header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"empleado_pwd","password":"clave123","nombre":"Empleado","role":"ROLE_EMPLOYEE"}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        long empleadoId = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        // Endpoint /me/password abierto a cualquier usuario autenticado, no solo ADMIN
        Sesion empleado = login("empleado_pwd", "clave123");
        mockMvc.perform(patch("/api/v1/usuarios/me/password")
                .header("Authorization", empleado.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\":\"clave123\",\"passwordNueva\":\"clave456\"}"))
            .andExpect(status().isNoContent());
        // ...pero el resto de /usuarios sigue siendo solo para ADMIN
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", empleado.bearer()))
            .andExpect(status().isForbidden());

        Sesion empleadoNueva = login("empleado_pwd", "clave456");
        assertThat(refrescar(empleadoNueva.refresh())).isEqualTo(200);

        mockMvc.perform(patch("/api/v1/usuarios/" + empleadoId + "/estado")
                .header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}"))
            .andExpect(status().isOk());

        assertThat(refrescar(empleadoNueva.refresh())).as("refresh de un usuario desactivado").isEqualTo(401);
        assertThat(intentarLogin("empleado_pwd", "clave456").getResponse().getStatus())
            .as("login de un usuario desactivado (antes daba 500)").isEqualTo(401);
    }
}
