package com.cerrajeria.sig.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Importacion de inventario contra una PostgreSQL real: el stock importado
 * queda en el Kardex, y un archivo con errores no deja nada a medias.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Integración: importar inventario desde CSV")
class ImportacionInventarioIntegrationTest {

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
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn();
        return "Bearer " + objectMapper.readTree(r.getResponse().getContentAsString()).get("accessToken").asString();
    }

    private JsonNode importar(String token, String csv, boolean confirmar) throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
            "archivo", "inventario.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        MvcResult r = mockMvc.perform(multipart("/api/v1/productos/importar")
                .file(archivo)
                .param("confirmar", String.valueOf(confirmar))
                .header("Authorization", token))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    private int contarProductos(String token, String termino) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/productos/buscar").param("q", termino)
                .header("Authorization", token))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).size();
    }

    @Test
    @DisplayName("Revisar, importar y encontrar el stock inicial en el Kárdex; un archivo con errores no crea nada")
    void importa_yRegistraKardex_todoONada() throws Exception {
        String admin = login("admin", "admin123");
        String csv = """
            codigo;nombre;tipo;stock;precio_compra;precio_venta
            IMP-001;Importada llave KW1;Llave virgen;40;1,20;3,00
            IMP-002;Importada candado 30;candado;0;10;18
            """;

        JsonNode revision = importar(admin, csv, false);
        assertThat(revision.get("errores")).isEmpty();
        assertThat(revision.get("productos")).hasSize(2);
        assertThat(revision.get("creados").asInt()).isZero();
        assertThat(contarProductos(admin, "Importada")).as("la revisión no guarda").isZero();

        JsonNode resultado = importar(admin, csv, true);
        assertThat(resultado.get("creados").asInt()).isEqualTo(2);

        MvcResult buscado = mockMvc.perform(get("/api/v1/productos/buscar").param("q", "IMP-001")
                .header("Authorization", admin))
            .andReturn();
        JsonNode llave = objectMapper.readTree(buscado.getResponse().getContentAsString()).get(0);
        assertThat(llave.get("cantidadStock").asInt()).isEqualTo(40);
        assertThat(llave.get("precioVenta").decimalValue()).isEqualByComparingTo("3.00");

        MvcResult kardex = mockMvc.perform(get("/api/v1/productos/" + llave.get("id").asLong() + "/kardex")
                .header("Authorization", admin))
            .andReturn();
        JsonNode movimientos = objectMapper.readTree(kardex.getResponse().getContentAsString());
        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).get("cantidad").asInt()).isEqualTo(40);
        assertThat(movimientos.get(0).get("motivo").asString()).isEqualTo("Stock inicial importado desde archivo");

        // Repetir el archivo: los códigos ya existen, y la fila nueva válida tampoco se crea (todo o nada)
        JsonNode repetido = importar(admin, csv + "IMP-003;Importada nueva;chapa;1;50;90\n", true);
        assertThat(repetido.get("creados").asInt()).isZero();
        assertThat(repetido.get("errores")).hasSize(2);
        assertThat(contarProductos(admin, "Importada")).isEqualTo(2);
    }

    @Test
    @DisplayName("Solo el administrador puede importar")
    void empleado_noPuedeImportar() throws Exception {
        String admin = login("admin", "admin123");
        mockMvc.perform(post("/api/v1/usuarios")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"empleado_imp","password":"clave123","nombre":"Empleado","role":"ROLE_EMPLOYEE"}
                    """))
            .andExpect(status().isCreated());
        String empleado = login("empleado_imp", "clave123");

        mockMvc.perform(multipart("/api/v1/productos/importar")
                .file(new MockMultipartFile("archivo", "x.csv", "text/csv", "a".getBytes(StandardCharsets.UTF_8)))
                .header("Authorization", empleado))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Planilla: descargar, llenar la cantidad y subirla como conteo; la compra de varios entra todo junto")
    void planillaYCompraDeVarios() throws Exception {
        String admin = login("admin", "admin123");
        importar(admin, """
            codigo;nombre;tipo;stock;precio_compra;precio_venta
            PLA-001;Planilla llave Yale;Llave virgen;200;1;3
            PLA-002;Planilla candado;candado;5;10;18
            """, true);

        MvcResult descarga = mockMvc.perform(get("/api/v1/productos/planilla").header("Authorization", admin))
            .andExpect(status().isOk())
            .andReturn();
        java.nio.charset.Charset ansi = java.nio.charset.Charset.forName("windows-1252");
        String planilla = new String(descarga.getResponse().getContentAsByteArray(), ansi);
        assertThat(planilla).contains("=\"PLA-001\";Planilla llave Yale;llave virgen;;200;");

        String llena = planilla.replace("Planilla llave Yale;llave virgen;;200;", "Planilla llave Yale;llave virgen;;200;1000");
        MvcResult subida = mockMvc.perform(multipart("/api/v1/productos/planilla")
                .file(new MockMultipartFile("archivo", "inventario.csv", "text/csv", llena.getBytes(ansi)))
                .param("modo", "CONTEO").param("confirmar", "true")
                .header("Authorization", admin))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode resultado = objectMapper.readTree(subida.getResponse().getContentAsString());
        assertThat(resultado.get("errores")).isEmpty();
        assertThat(resultado.get("actualizados").asInt()).isEqualTo(1);

        JsonNode llave = objectMapper.readTree(mockMvc.perform(get("/api/v1/productos/buscar").param("q", "PLA-001")
            .header("Authorization", admin)).andReturn().getResponse().getContentAsString()).get(0);
        assertThat(llave.get("cantidadStock").asInt()).isEqualTo(1000);
        JsonNode kardex = objectMapper.readTree(mockMvc.perform(get("/api/v1/productos/" + llave.get("id").asLong() + "/kardex")
            .header("Authorization", admin)).andReturn().getResponse().getContentAsString());
        assertThat(kardex.get(0).get("cantidad").asInt()).isEqualTo(800);
        assertThat(kardex.get(0).get("motivo").asString()).isEqualTo("Conteo físico (planilla)");

        // Compra de varios al mismo distribuidor
        long proveedorId = objectMapper.readTree(mockMvc.perform(post("/api/v1/proveedores")
                .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Distribuidora Planilla\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        JsonNode candado = objectMapper.readTree(mockMvc.perform(get("/api/v1/productos/buscar").param("q", "PLA-002")
            .header("Authorization", admin)).andReturn().getResponse().getContentAsString()).get(0);
        mockMvc.perform(post("/api/v1/compras/lote").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"proveedorId":%d,"pagarDeCaja":false,"items":[
                      {"productoId":%d,"cantidad":100,"precioUnitario":0.9},
                      {"productoId":%d,"cantidad":2,"precioUnitario":11}]}
                    """.formatted(proveedorId, llave.get("id").asLong(), candado.get("id").asLong())))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/compras/lote").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"proveedorId":%d,"pagarDeCaja":false,"items":[{"productoId":%d,"cantidad":0,"precioUnitario":1}]}
                    """.formatted(proveedorId, candado.get("id").asLong())))
            .andExpect(status().isBadRequest());
        assertThat(contarStock(admin, "PLA-001")).isEqualTo(1100);
        assertThat(contarStock(admin, "PLA-002")).isEqualTo(7);
    }

    private int contarStock(String token, String codigo) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/v1/productos/buscar").param("q", codigo)
            .header("Authorization", token)).andReturn().getResponse().getContentAsString())
            .get(0).get("cantidadStock").asInt();
    }
}
