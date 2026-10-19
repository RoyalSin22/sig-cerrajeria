package com.cerrajeria.sig.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceso desde el celular por Tailscale: el navegador manda el origen
 * https://&lt;pc&gt;.&lt;red&gt;.ts.net y, detras de nginx, el backend lo ve como
 * otro origen. Si no estuviera permitido, ni el login funcionaria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Integración: orígenes permitidos (PC y celular por Tailscale)")
class AccesoCelularIntegrationTest {

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

    private ResultActions loginDesde(String origen) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
            .header("Origin", origen)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"admin123\"}"));
    }

    @Test
    @DisplayName("Desde la dirección HTTPS de Tailscale se puede iniciar sesión, con la cookie de sesión")
    void tailscale_permitido() throws Exception {
        String origen = "https://cerrajeria.tail1a2b3c.ts.net";
        loginDesde(origen)
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", origen))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Un origen que no está en la lista se rechaza (incluida una IP del wifi)")
    void otrosOrigenes_rechazados() throws Exception {
        loginDesde("https://sitio-ajeno.example.com").andExpect(status().isForbidden());
        loginDesde("https://ts.net.sitio-ajeno.example.com").andExpect(status().isForbidden());
        loginDesde("http://192.168.1.50:8081").andExpect(status().isForbidden());
        loginDesde("http://cerrajeria.tail1a2b3c.ts.net").andExpect(status().isForbidden()); // sin HTTPS
    }

    @Test
    @DisplayName("El webhook de WhatsApp rechaza mensajes sin la firma de Meta")
    void webhookWhatsApp_sinFirma_403() throws Exception {
        mockMvc.perform(post("/api/v1/whatsapp/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entry\":[{\"changes\":[{\"value\":{\"messages\":[{\"from\":\"51999999999\","
                    + "\"type\":\"text\",\"text\":{\"body\":\"registra una venta\"}}]}}]}]}"))
            .andExpect(status().isForbidden());
    }
}
