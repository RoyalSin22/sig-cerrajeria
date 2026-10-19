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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobro de servicios a domicilio, cancelacion de ordenes con devolucion y su
 * reflejo en caja y reportes, contra PostgreSQL real (migracion V9).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Integración: cobro a domicilio, cancelación de órdenes y reporte de ingresos")
class IngresosTallerDomicilioIntegrationTest {

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

    private String token;

    private JsonNode llamar(String metodo, String url, String body, ResultMatcher esperado) throws Exception {
        var req = metodo.equals("GET") ? get(url) : post(url);
        req.header("Authorization", token);
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        MvcResult r = mockMvc.perform(req).andExpect(esperado).andReturn();
        String json = r.getResponse().getContentAsString();
        return json.isEmpty() ? null : objectMapper.readTree(json);
    }

    private String login(String usuario, String clave) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(usuario, clave)))
            .andExpect(status().isOk()).andReturn();
        return "Bearer " + objectMapper.readTree(r.getResponse().getContentAsString()).get("accessToken").asString();
    }

    private JsonNode caja() throws Exception {
        return llamar("GET", "/api/v1/caja/abierta", null, status().isOk());
    }

    @Test
    @DisplayName("Domicilio cobrado y órdenes canceladas cuadran en la caja y suman en el reporte")
    void flujoCompleto() throws Exception {
        token = login("admin", "admin123");
        llamar("POST", "/api/v1/caja/abrir", "{\"montoApertura\": 100}", status().isCreated());
        long clienteId = llamar("POST", "/api/v1/clientes",
            "{\"nombre\":\"Rosa Quispe\",\"telefono\":\"987654321\"}", status().isCreated()).get("id").asLong();

        // --- Domicilio: se cobra al realizarlo, en la caja de quien lo realiza ---
        long servicioId = llamar("POST", "/api/v1/servicios-domicilio", """
            {"clienteId":%d,"direccion":"Av. Arequipa 123","telefono":"987654321",
             "fecha":"%s","hora":"10:00","tipoServicio":"Apertura de puerta"}
            """.formatted(clienteId, LocalDate.now()), status().isCreated()).get("id").asLong();
        JsonNode realizado = llamar("POST", "/api/v1/servicios-domicilio/" + servicioId + "/realizar",
            "{\"monto\":80,\"metodoPago\":\"YAPE\"}", status().isOk());
        assertThat(realizado.get("estado").asString()).isEqualTo("REALIZADO");
        assertThat(realizado.get("realizadoPor").asString()).isNotBlank();
        assertThat(caja().get("montoYape").decimalValue()).isEqualByComparingTo("80");
        assertThat(caja().get("efectivoEsperado").decimalValue()).as("Yape no va al cajón").isEqualByComparingTo("100");
        llamar("POST", "/api/v1/servicios-domicilio/" + servicioId + "/realizar",
            "{\"monto\":80,\"metodoPago\":\"YAPE\"}", status().isBadRequest()); // no se cobra dos veces

        // --- Orden cancelada devolviendo el adelanto en efectivo ---
        long orden1 = llamar("POST", "/api/v1/ordenes", """
            {"clienteId":%d,"productoRecibido":"Chapa Forte","problema":"No cierra",
             "precioTotal":150,"adelanto":50,"metodoPagoAdelanto":"EFECTIVO"}
            """.formatted(clienteId), status().isCreated()).get("id").asLong();
        assertThat(caja().get("efectivoEsperado").decimalValue()).isEqualByComparingTo("150");
        JsonNode cancelada = llamar("POST", "/api/v1/ordenes/" + orden1 + "/cancelar",
            "{\"motivo\":\"Se arrepintió\",\"devolverAdelanto\":true,\"metodoDevolucion\":\"EFECTIVO\"}",
            status().isOk());
        assertThat(cancelada.get("estado").asString()).isEqualTo("CANCELADO");
        assertThat(cancelada.get("montoPagado").decimalValue()).isEqualByComparingTo("0");
        assertThat(caja().get("efectivoEsperado").decimalValue()).as("se devolvió del cajón").isEqualByComparingTo("100");
        llamar("POST", "/api/v1/ordenes/" + orden1 + "/pagos",
            "{\"monto\":10,\"metodoPago\":\"EFECTIVO\"}", status().isBadRequest()); // cancelada: no cobra

        // --- Orden abandonada: se cancela sin devolver, el adelanto se queda ---
        long orden2 = llamar("POST", "/api/v1/ordenes", """
            {"clienteId":%d,"productoRecibido":"Candado","problema":"Sin llave",
             "precioTotal":60,"adelanto":20,"metodoPagoAdelanto":"PLIN"}
            """.formatted(clienteId), status().isCreated()).get("id").asLong();
        llamar("POST", "/api/v1/ordenes/" + orden2 + "/cancelar",
            "{\"motivo\":\"Nunca la recogió\",\"devolverAdelanto\":false}", status().isOk());
        assertThat(caja().get("montoPlin").decimalValue()).isEqualByComparingTo("20");

        // --- Reporte: taller neto de devoluciones + domicilio ---
        JsonNode reporte = llamar("GET", "/api/v1/reportes/ventas?desde=%s&hasta=%s"
            .formatted(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)), null, status().isOk());
        assertThat(reporte.get("ingresosTaller").decimalValue()).isEqualByComparingTo("20"); // 50 - 50 + 20
        assertThat(reporte.get("ingresosDomicilio").decimalValue()).isEqualByComparingTo("80");
        assertThat(reporte.get("serviciosDomicilioRealizados").asLong()).isEqualTo(1);
        assertThat(reporte.get("ingresosTotales").decimalValue()).isEqualByComparingTo("100");

        String rango = "desde=%s&hasta=%s".formatted(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        // --- RF-43: servicios realizados (las consultas JPQL contra PostgreSQL real) ---
        JsonNode servicios = llamar("GET", "/api/v1/reportes/servicios?" + rango, null, status().isOk());
        JsonNode apertura = null;
        for (JsonNode g : servicios.path("domicilio").path("porTipo")) {
            if ("Apertura de puerta".equals(g.get("nombre").asString())) apertura = g;
        }
        assertThat(apertura).as("agrupado por tipo de servicio").isNotNull();
        assertThat(apertura.get("cantidad").asLong()).isEqualTo(1);
        assertThat(apertura.get("total").decimalValue()).isEqualByComparingTo("80");
        assertThat(servicios.path("domicilio").path("porTecnico")).isNotEmpty();
        assertThat(servicios.path("taller").get("recibidas").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(servicios.path("taller").get("ingresosNetos").decimalValue()).isEqualByComparingTo("20");

        // --- RF-44: ranking de clientes (taller neto de la devolución + domicilio) ---
        JsonNode clientes = llamar("GET", "/api/v1/reportes/clientes?" + rango, null, status().isOk());
        JsonNode primero = clientes.path("ranking").get(0);
        assertThat(primero.get("nombre").asString()).isEqualTo("Rosa Quispe");
        assertThat(primero.path("taller").get("cantidad").asLong()).as("dos órdenes con pagos").isEqualTo(2);
        assertThat(primero.path("taller").get("total").decimalValue()).isEqualByComparingTo("20");
        assertThat(primero.path("domicilio").get("total").decimalValue()).isEqualByComparingTo("80");
        assertThat(primero.path("ventas").get("cantidad").asLong()).isZero();
        assertThat(primero.get("total").decimalValue()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("Quien realiza el servicio sin tener su caja abierta no puede cobrarlo")
    void realizarSinCaja_409() throws Exception {
        token = login("admin", "admin123");
        llamar("POST", "/api/v1/usuarios", """
            {"username":"tecnico_dom","password":"clave123","nombre":"Técnico","role":"ROLE_EMPLOYEE"}
            """, status().isCreated());
        long clienteId = llamar("POST", "/api/v1/clientes",
            "{\"nombre\":\"Luis Ramos\",\"telefono\":\"912345678\"}", status().isCreated()).get("id").asLong();
        long servicioId = llamar("POST", "/api/v1/servicios-domicilio", """
            {"clienteId":%d,"direccion":"Jr. Lampa 456","telefono":"912345678",
             "fecha":"%s","hora":"15:00","tipoServicio":"Cambio de chapa"}
            """.formatted(clienteId, LocalDate.now()), status().isCreated()).get("id").asLong();

        token = login("tecnico_dom", "clave123");
        llamar("POST", "/api/v1/servicios-domicilio/" + servicioId + "/realizar",
            "{\"monto\":120,\"metodoPago\":\"EFECTIVO\"}", status().isConflict());
        // Sin cobro (garantía) sí se puede, porque no mueve dinero
        llamar("POST", "/api/v1/servicios-domicilio/" + servicioId + "/realizar",
            "{\"monto\":0}", status().isOk());
    }
}
