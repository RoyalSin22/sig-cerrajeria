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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integridad contra PostgreSQL real: ventas simultaneas del mismo producto,
 * cierre de caja que no cuadra, garantia de taller con material, y el detalle
 * de la auditoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Integración: concurrencia, cierre de caja, garantías y auditoría")
class IntegridadIntegrationTest {

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

    private JsonNode llamar(String token, MockHttpServletRequestBuilder req, String body, ResultMatcher esperado)
            throws Exception {
        req.header("Authorization", token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        String json = mockMvc.perform(req).andExpect(esperado).andReturn().getResponse().getContentAsString();
        return json.isEmpty() ? null : objectMapper.readTree(json);
    }

    private String login(String usuario, String clave) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(usuario, clave)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(json).get("accessToken").asString();
    }

    /** Usuario propio por test: cada uno con su caja, sin depender del orden de ejecucion. */
    private String usuarioConCaja(String username, String montoApertura) throws Exception {
        String admin = login("admin", "admin123");
        llamar(admin, post("/api/v1/usuarios"), """
            {"username":"%s","password":"clave123","nombre":"%s","role":"ROLE_ADMIN"}
            """.formatted(username, username), status().isCreated());
        String token = login(username, "clave123");
        llamar(token, post("/api/v1/caja/abrir"), "{\"montoApertura\": " + montoApertura + "}", status().isCreated());
        return token;
    }

    private long crearProducto(String token, String codigo, int stock, String precioVenta) throws Exception {
        return llamar(token, post("/api/v1/productos"), """
            {"codigo":"%s","nombre":"Producto %s","tipoProducto":"CANDADO","cantidadStock":%d,
             "precioCompra":1,"precioVenta":%s}
            """.formatted(codigo, codigo, stock, precioVenta), status().isCreated()).get("id").asLong();
    }

    @Test
    @DisplayName("Ventas simultáneas del mismo producto: ninguna resta se pierde, ni en el stock ni en la caja")
    void ventasSimultaneas_noPierdenStock() throws Exception {
        String token = usuarioConCaja("cajero_concurrente", "0");
        long productoId = crearProducto(token, "CONC-1", 30, "2");

        int ventas = 30;
        ExecutorService hilos = Executors.newFixedThreadPool(8);
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (int i = 0; i < ventas; i++) {
            tareas.add(() -> mockMvc.perform(post("/api/v1/ventas").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"metodoPago\":\"EFECTIVO\",\"items\":[{\"productoId\":%d,\"cantidad\":1}]}"
                        .formatted(productoId)))
                .andReturn().getResponse().getStatus());
        }
        List<Integer> estados = new ArrayList<>();
        for (Future<Integer> f : hilos.invokeAll(tareas)) estados.add(f.get());
        hilos.shutdown();

        assertThat(estados).as("todas las ventas se registran").containsOnly(201);
        JsonNode producto = llamar(token, get("/api/v1/productos/" + productoId), null, status().isOk());
        assertThat(producto.get("cantidadStock").asInt()).as("30 - 30 ventas").isZero();
        JsonNode kardex = llamar(token, get("/api/v1/productos/" + productoId + "/kardex"), null, status().isOk());
        assertThat(kardex).hasSize(ventas + 1); // + el stock inicial
        JsonNode caja = llamar(token, get("/api/v1/caja/abierta"), null, status().isOk());
        assertThat(caja.get("montoIngresos").decimalValue()).isEqualByComparingTo("60");

        // Sin stock: la venta 31 se rechaza (y no deja el stock en negativo)
        llamar(token, post("/api/v1/ventas"),
            "{\"metodoPago\":\"EFECTIVO\",\"items\":[{\"productoId\":%d,\"cantidad\":1}]}".formatted(productoId),
            status().is4xxClientError());
    }

    @Test
    @DisplayName("Cierre de caja que no cuadra: exige explicación y la diferencia queda guardada")
    void cierreConDiferencia() throws Exception {
        String token = usuarioConCaja("cajero_cierre", "100");
        llamar(token, post("/api/v1/caja/cerrar"), "{\"montoContado\": 95}", status().isBadRequest());
        llamar(token, post("/api/v1/caja/cerrar"),
            "{\"montoContado\": 95, \"observacion\": \"Vuelto mal dado\"}", status().isOk());

        JsonNode historial = llamar(token, get("/api/v1/caja/historial"), null, status().isOk()).get("content");
        assertThat(historial.get(0).get("diferenciaCierre").decimalValue()).isEqualByComparingTo("-5");
        assertThat(historial.get(0).get("observacionCierre").asString()).isEqualTo("Vuelto mal dado");
    }

    @Test
    @DisplayName("Garantía: orden sin cobro sobre la entregada, con material por el Kárdex y en la auditoría")
    void garantiaConMaterial() throws Exception {
        String token = usuarioConCaja("tecnico_garantia", "0");
        long clienteId = llamar(token, post("/api/v1/clientes"),
            "{\"nombre\":\"Marta Salas\",\"telefono\":\"955111222\"}", status().isCreated()).get("id").asLong();
        long piezaId = crearProducto(token, "PIEZA-1", 5, "15");

        JsonNode orden = llamar(token, post("/api/v1/ordenes"), """
            {"clienteId":%d,"productoRecibido":"Chapa Yale","problema":"No gira","precioTotal":60}
            """.formatted(clienteId), status().isCreated());
        long ordenId = orden.get("id").asLong();
        llamar(token, post("/api/v1/ordenes/" + ordenId + "/garantia"), "{\"problema\":\"x\"}",
            status().isBadRequest()); // todavia no se entrego
        llamar(token, post("/api/v1/ordenes/" + ordenId + "/entregar"),
            "{\"monto\":60,\"metodoPago\":\"EFECTIVO\"}", status().isOk());

        JsonNode garantia = llamar(token, post("/api/v1/ordenes/" + ordenId + "/garantia"),
            "{\"problema\":\"Volvió a trabarse\"}", status().isCreated());
        long garantiaId = garantia.get("id").asLong();
        assertThat(garantia.get("garantia").asBoolean()).isTrue();
        assertThat(garantia.get("precioTotal").decimalValue()).isZero();
        assertThat(garantia.get("numeroOrdenOrigen").asString()).isEqualTo(orden.get("numeroOrden").asString());

        JsonNode materiales = llamar(token, post("/api/v1/ordenes/" + garantiaId + "/materiales"),
            "{\"productoId\":%d,\"cantidad\":1}".formatted(piezaId), status().isOk());
        assertThat(materiales).singleElement().satisfies(m -> assertThat(m.get("cantidad").asInt()).isEqualTo(1));
        JsonNode pieza = llamar(token, get("/api/v1/productos/" + piezaId), null, status().isOk());
        assertThat(pieza.get("cantidadStock").asInt()).isEqualTo(4);

        // Se entrega sin cobrar y la caja no se mueve
        JsonNode cajaAntes = llamar(token, get("/api/v1/caja/abierta"), null, status().isOk());
        llamar(token, post("/api/v1/ordenes/" + garantiaId + "/entregar"), null, status().isOk());
        JsonNode cajaDespues = llamar(token, get("/api/v1/caja/abierta"), null, status().isOk());
        assertThat(cajaDespues.get("montoIngresos").decimalValue())
            .isEqualByComparingTo(cajaAntes.get("montoIngresos").decimalValue());

        JsonNode auditoria = llamar(token, get("/api/v1/auditoria").param("entidad", "OrdenTrabajo")
            .param("entidadId", String.valueOf(garantiaId)), null, status().isOk());
        assertThat(auditoria.get("content").toString()).contains("Garantía", "Volvió a trabarse");
    }

    @Test
    @DisplayName("Auditoría: un cambio de precio guarda el valor anterior y el nuevo")
    void auditoriaDeCambioDePrecio() throws Exception {
        String admin = login("admin", "admin123");
        long id = crearProducto(admin, "AUD-1", 1, "10");
        llamar(admin, put("/api/v1/productos/" + id), """
            {"nombre":"Producto AUD-1","tipoProducto":"CANDADO","precioCompra":1,"precioVenta":12.5}
            """, status().isOk());

        JsonNode registros = llamar(admin, get("/api/v1/auditoria").param("entidad", "Producto")
            .param("entidadId", String.valueOf(id)), null, status().isOk()).get("content");
        JsonNode edicion = null;
        for (JsonNode r : registros) if ("EDITAR".equals(r.get("accion").asString())) edicion = r;
        assertThat(edicion).isNotNull();
        JsonNode cambio = edicion.path("detalle").path("cambios").path("precioVenta");
        assertThat(cambio.get("antes").asString()).isEqualTo("10.00");
        assertThat(cambio.get("despues").asString()).isEqualTo("12.50");
    }

    @Test
    @DisplayName("Anular una venta devuelve el stock y el dinero con la caja bloqueada, y queda auditada con su detalle")
    void anulacionConBloqueo() throws Exception {
        String token = usuarioConCaja("cajero_anula", "0");
        long productoId = crearProducto(token, "ANU-1", 5, "8");
        long ventaId = llamar(token, post("/api/v1/ventas"),
            "{\"metodoPago\":\"YAPE\",\"items\":[{\"productoId\":%d,\"cantidad\":2}]}".formatted(productoId),
            status().isCreated()).get("id").asLong();

        llamar(token, post("/api/v1/ventas/" + ventaId + "/anular"), null, status().isOk());

        assertThat(llamar(token, get("/api/v1/productos/" + productoId), null, status().isOk())
            .get("cantidadStock").asInt()).isEqualTo(5);
        JsonNode caja = llamar(token, get("/api/v1/caja/abierta"), null, status().isOk());
        assertThat(caja.get("montoYape").decimalValue()).isZero();
        JsonNode auditoria = llamar(token, get("/api/v1/auditoria").param("entidad", "Venta")
            .param("entidadId", String.valueOf(ventaId)), null, status().isOk());
        assertThat(auditoria.get("content").get(0).path("detalle").path("descripcion").asString())
            .contains("anulada", "S/ 16", "2 × Producto ANU-1");
    }
}
