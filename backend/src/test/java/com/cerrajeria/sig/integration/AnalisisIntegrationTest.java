package com.cerrajeria.sig.integration;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El Excel para Power BI contra PostgreSQL real: lo vendido en el sistema y las
 * ventas anteriores cargadas desde archivo llegan con sus tipos (fechas y
 * montos) y cada hoja es una tabla de Excel con nombre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Integración: datos para Power BI y ventas anteriores")
class AnalisisIntegrationTest {

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

    private JsonNode cargar(String token, String csv, boolean confirmar) throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "ventas.csv", "text/csv",
            csv.getBytes(StandardCharsets.UTF_8));
        String json = mockMvc.perform(multipart("/api/v1/analisis/ventas-anteriores").file(archivo)
                .param("confirmar", String.valueOf(confirmar)).header("Authorization", token))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    @Test
    @DisplayName("Ventas del sistema y anteriores llegan al Excel con fechas, montos y tablas con nombre")
    void excelPowerBi_conVentasDelSistemaYAnteriores() throws Exception {
        String admin = login("admin", "admin123");
        llamar(admin, post("/api/v1/caja/abrir"), "{\"montoApertura\": 50}", status().isCreated());
        long llave = llamar(admin, post("/api/v1/productos"), """
            {"codigo":"PBI-CASA","nombre":"Llave casa PBI","tipoProducto":"LLAVE_VIRGEN","marca":"CV",
             "cantidadStock":10,"precioCompra":0.9,"precioVenta":1.5,"precioDuplicado":2.5}
            """, status().isCreated()).get("id").asLong();

        llamar(admin, post("/api/v1/ventas"), """
            {"metodoPago":"EFECTIVO","items":[
              {"productoId":%d,"cantidad":2,"duplicado":true},
              {"cantidad":1,"descripcion":"Abrir candado","precio":10}]}
            """.formatted(llave), status().isCreated());
        llamar(admin, post("/api/v1/ventas"), """
            {"metodoPago":"MIXTO","items":[{"productoId":%d,"cantidad":1}],
             "desglosePago":[{"metodoPago":"EFECTIVO","monto":1},{"metodoPago":"YAPE","monto":0.5}]}
            """.formatted(llave), status().isCreated());

        // Un archivo con una fila mala no carga nada y dice cual fila es
        JsonNode malo = cargar(admin, """
            Fecha;Producto;Código;Cantidad;Precio;Método de pago
            15/03/2025;Llave de casa;PBI-CASA;10;2,50;Efectivo
            16/03/2025;Candado Forte 40;;1;;yape
            """, true);
        assertThat(malo.get("cargadas").asInt()).isZero();
        assertThat(malo.get("errores").get(0).get("fila").asInt()).isEqualTo(3);
        assertThat(llamar(admin, get("/api/v1/analisis/ventas-anteriores"), null, status().isOk())
            .get("filas").asLong()).isZero();

        String csv = """
            Fecha;Producto;Código;Cantidad;Precio;Método de pago
            15/03/2025;Llave de casa;PBI-CASA;10;2,50;Efectivo
            16/03/2025;Candado Forte 40;;1;S/ 30.00;yape
            17/03/25;Llavero;;3;4;
            """;
        JsonNode revision = cargar(admin, csv, false);
        assertThat(revision.get("errores")).isEmpty();
        assertThat(revision.get("cargadas").asInt()).as("revisar no guarda").isZero();
        assertThat(revision.get("enlazadas").asInt()).as("PBI-CASA por su código").isEqualTo(1);
        assertThat(revision.get("total").decimalValue()).isEqualByComparingTo("67"); // 25 + 30 + 12
        assertThat(revision.get("hasta").asString()).isEqualTo("2025-03-17");
        assertThat(cargar(admin, csv, true).get("cargadas").asInt()).isEqualTo(3);

        // El empleado no descarga los datos del negocio
        llamar(admin, post("/api/v1/usuarios"), """
            {"username":"empleado_pbi","password":"clave123","nombre":"Empleado","role":"ROLE_EMPLOYEE"}
            """, status().isCreated());
        String empleado = login("empleado_pbi", "clave123");
        mockMvc.perform(get("/api/v1/analisis/power-bi").param("desde", "2025-01-01")
                .param("hasta", LocalDate.now().toString()).header("Authorization", empleado))
            .andExpect(status().isForbidden());

        byte[] xlsx = mockMvc.perform(get("/api/v1/analisis/power-bi").param("desde", "2025-01-01")
                .param("hasta", LocalDate.now().toString()).header("Authorization", admin))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        try (ReadableWorkbook libro = new ReadableWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(libro.getSheets().map(Sheet::getName)).containsExactly("Ventas", "Cobros", "Taller",
                "Domicilio", "Productos", "Kardex", "Compras", "Cajas", "MovimientosCaja", "Léeme");

            List<Row> ventas = libro.findSheet("Ventas").orElseThrow().read();
            Map<String, Integer> col = columnas(ventas.get(0));
            List<Row> filas = ventas.subList(1, ventas.size());
            assertThat(filas).hasSize(6); // 3 lineas del sistema + 3 anteriores

            Row duplicado = unica(filas, col.get("Línea"), "Duplicado");
            assertThat(duplicado.getCellAsNumber(col.get("Cantidad"))).hasValueSatisfying(
                n -> assertThat(n).isEqualByComparingTo("2"));
            assertThat(duplicado.getCellAsNumber(col.get("Subtotal"))).hasValueSatisfying(
                n -> assertThat(n).isEqualByComparingTo("5"));
            assertThat(duplicado.getCellAsDate(col.get("Fecha"))).as("fecha como fecha, no texto")
                .hasValueSatisfying(f -> assertThat(f.toLocalDate()).isEqualTo(LocalDate.now()));
            assertThat(duplicado.getCellText(col.get("Tipo"))).isEqualTo("Llave virgen");
            assertThat(unica(filas, col.get("Línea"), "Servicio").getCellText(col.get("Producto")))
                .isEqualTo("Abrir candado");

            List<Row> anteriores = filas.stream()
                .filter(r -> "Anterior".equals(r.getCellText(col.get("Origen")))).toList();
            assertThat(anteriores).hasSize(3);
            assertThat(anteriores.get(0).getCellText(col.get("Producto")))
                .as("enlazada: toma el nombre del inventario").isEqualTo("Llave casa PBI");
            assertThat(anteriores.get(1).getCellText(col.get("Línea")))
                .as("no coincidió con el inventario").isEqualTo("Sin producto");
            assertThat(anteriores.get(0).getCellAsDate(col.get("Fecha")))
                .hasValueSatisfying(f -> assertThat(f.toLocalDate()).isEqualTo(LocalDate.of(2025, 3, 15)));

            List<Row> cobros = libro.findSheet("Cobros").orElseThrow().read();
            Map<String, Integer> cc = columnas(cobros.get(0));
            assertThat(cobros).hasSize(1 + 3); // venta en efectivo + mixto en dos lineas
            assertThat(unica(cobros.subList(1, cobros.size()), cc.get("Método de pago"), "Yape")
                .getCellAsNumber(cc.get("Monto"))).hasValueSatisfying(n -> assertThat(n).isEqualByComparingTo("0.5"));
        }

        assertThat(tablasDelLibro(xlsx)).contains("Ventas", "Cobros", "Productos", "Cajas", "Kardex");

        assertThat(llamar(admin, delete("/api/v1/analisis/ventas-anteriores"), null, status().isOk())
            .get("borradas").asLong()).isEqualTo(3);
        assertThat(llamar(admin, get("/api/v1/analisis/ventas-anteriores"), null, status().isOk())
            .get("filas").asLong()).isZero();
    }

    private static Map<String, Integer> columnas(Row cabecera) {
        return java.util.stream.IntStream.range(0, cabecera.getCellCount()).boxed()
            .collect(Collectors.toMap(cabecera::getCellText, i -> i));
    }

    private static Row unica(List<Row> filas, int columna, String valor) {
        List<Row> encontradas = filas.stream().filter(r -> valor.equals(r.getCellText(columna))).toList();
        assertThat(encontradas).as("una fila con " + valor).hasSize(1);
        return encontradas.get(0);
    }

    /** Power BI lista las tablas de Excel por nombre: estan en xl/tables/*.xml (leidas como Excel, por el indice del zip). */
    private static List<String> tablasDelLibro(byte[] xlsx) throws Exception {
        java.nio.file.Path archivo = java.nio.file.Files.createTempFile("power-bi", ".xlsx");
        List<String> nombres = new ArrayList<>();
        try {
            java.nio.file.Files.write(archivo, xlsx);
            try (ZipFile zip = new ZipFile(archivo.toFile())) {
                for (ZipEntry e : java.util.Collections.list(zip.entries())) {
                    if (e.getName().startsWith("xl/tables/")) {
                        String xml = new String(zip.getInputStream(e).readAllBytes(), StandardCharsets.UTF_8);
                        var m = java.util.regex.Pattern.compile("displayName=\"([^\"]+)\"").matcher(xml);
                        if (m.find()) nombres.add(m.group(1));
                    }
                }
            }
        } finally {
            java.nio.file.Files.deleteIfExists(archivo);
        }
        return nombres;
    }
}
