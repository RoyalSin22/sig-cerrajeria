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
}
