package com.cerrajeria.sig.integration;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integracion end-to-end contra una PostgreSQL real (Testcontainers,
 * con Flyway aplicando V1-V5 al arrancar): confirma que las reglas de negocio
 * 1 y 2 se cumplen atravesando toda la pila (controlador -> servicio -> BD),
 * no solo en aislamiento con mocks como en los tests unitarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Flujo de integración: caja -> producto -> venta -> Kardex")
class VentaFlowIntegrationTest {

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

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asString();
    }

    @Test
    @DisplayName("Una venta descuenta stock (con Kardex) y suma el ingreso a la caja activa")
    void flujoCompleto_ventaDescuentaStockYActualizaCaja() throws Exception {
        String token = login("admin", "admin123");
        String auth = "Bearer " + token;

        mockMvc.perform(post("/api/v1/caja/abrir")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"montoApertura\": 100}"))
            .andExpect(status().isCreated());

        String productoBody = """
            {
              "codigo": "TEST-001", "nombre": "Llave de prueba", "tipoProducto": "LLAVE_VIRGEN",
              "cantidadStock": 10, "stockMinimo": 2, "precioCompra": 1.0, "precioVenta": 5.0
            }
            """;
        String productoResponse = mockMvc.perform(post("/api/v1/productos")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productoBody))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long productoId = objectMapper.readTree(productoResponse).get("id").asLong();

        String ventaBody = """
            {"metodoPago": "EFECTIVO", "items": [{"productoId": %d, "cantidad": 3}]}
            """.formatted(productoId);
        mockMvc.perform(post("/api/v1/ventas")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ventaBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.montoTotal").value(15.0));

        // El stock bajo de 10 a 7 (regla de negocio 2: descuento inmediato)
        mockMvc.perform(get("/api/v1/productos/" + productoId).header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidadStock").value(7));

        // El Kardex registro la salida
        mockMvc.perform(get("/api/v1/productos/" + productoId + "/kardex").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tipoMovimiento").value("SALIDA"))
            .andExpect(jsonPath("$[0].cantidad").value(-3));

        // La caja reflejo el ingreso (regla de negocio 1: venta ligada a la caja activa)
        mockMvc.perform(get("/api/v1/caja/abierta").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.montoIngresos").value(15.0))
            .andExpect(jsonPath("$.efectivoEsperado").value(115.0));

        // Un cobro por Yape suma al total de la caja, pero no al efectivo del cajon
        mockMvc.perform(post("/api/v1/ventas")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"metodoPago": "YAPE", "items": [{"productoId": %d, "cantidad": 2}]}
                    """.formatted(productoId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/caja/abierta").header("Authorization", auth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.montoIngresos").value(25.0))
            .andExpect(jsonPath("$.montoYape").value(10.0))
            .andExpect(jsonPath("$.saldoEsperado").value(125.0))
            .andExpect(jsonPath("$.efectivoEsperado").value(115.0));
    }

    @Test
    @DisplayName("Una venta se rechaza si el usuario no tiene una caja abierta (regla de negocio 1)")
    void venta_esRechazada_siNoHayCajaAbierta() throws Exception {
        String tokenAdmin = login("admin", "admin123");

        // Usuario nuevo y aislado: garantiza que nunca tuvo caja abierta,
        // sin depender del orden de ejecucion de los tests.
        mockMvc.perform(post("/api/v1/usuarios")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"empleado_sin_caja","password":"clave123","nombre":"Empleado Prueba","role":"ROLE_EMPLOYEE"}
                    """))
            .andExpect(status().isCreated());

        String tokenEmpleado = login("empleado_sin_caja", "clave123");

        mockMvc.perform(post("/api/v1/ventas")
                .header("Authorization", "Bearer " + tokenEmpleado)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"metodoPago": "EFECTIVO", "items": [{"productoId": 999999, "cantidad": 1}]}
                    """))
            .andExpect(status().is4xxClientError());
    }
}
