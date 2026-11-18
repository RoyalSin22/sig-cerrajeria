package com.cerrajeria.sig.integration;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los Excel con formato contra PostgreSQL real: la plantilla de inventario y la planilla de
 * cantidades se descargan, se llenan y se vuelven a subir como .xlsx; y los cuatro reportes
 * salen con los numeros del periodo, tablas con nombre y totales que siguen al filtro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Integración: plantillas y reportes en Excel")
class ExcelIntegrationTest {

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
    @Autowired private JdbcTemplate jdbc;

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

    /** Descarga un .xlsx y comprueba que sea un ZIP estandar: lo que LibreOffice y otros lectores exigen. */
    private byte[] descargar(String token, String url, String... params) throws Exception {
        MockHttpServletRequestBuilder req = get(url).header("Authorization", token);
        for (int i = 0; i < params.length; i += 2) req.param(params[i], params[i + 1]);
        byte[] xlsx = mockMvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new ByteArrayInputStream(xlsx))) {
            ZipEntry primera = zip.getNextEntry();
            assertThat(primera.getName()).as("[Content_Types].xml va primero").isEqualTo("[Content_Types].xml");
            for (ZipEntry e = primera; e != null; e = zip.getNextEntry()) {
                assertThat(zip.readAllBytes()).as("lectura en streaming de " + e.getName()).isNotEmpty();
            }
        }
        String muestras = System.getProperty("muestras"); // para revisar el aspecto a mano: -Dmuestras=carpeta
        if (muestras != null) {
            java.nio.file.Files.write(java.nio.file.Path.of(muestras, url.replaceAll("[^a-z]+", "-") + ".xlsx"), xlsx);
        }
        return xlsx;
    }

    private JsonNode subir(String token, String url, byte[] xlsx, String... params) throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "datos.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);
        var req = multipart(url).file(archivo).header("Authorization", token);
        for (int i = 0; i < params.length; i += 2) req.param(params[i], params[i + 1]);
        return objectMapper.readTree(mockMvc.perform(req).andExpect(status().isOk()).andReturn().getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
    }

    private static ReadableWorkbook abrir(byte[] xlsx) throws Exception {
        return new ReadableWorkbook(new ByteArrayInputStream(xlsx), new ReadingOptions(true, false));
    }

    private static List<Row> filas(ReadableWorkbook libro, String hoja) throws Exception {
        return libro.findSheet(hoja).orElseThrow(() -> new AssertionError("Falta la hoja " + hoja)).read();
    }

    /** El numero de la columna B de la fila de un resumen cuya columna A dice {@code etiqueta}. */
    private static BigDecimal kpi(ReadableWorkbook libro, String etiqueta) throws Exception {
        return num(filas(libro, "Resumen").stream()
            .filter(r -> etiqueta.equals(r.getCellText(0)))
            .findFirst().orElseThrow(() -> new AssertionError("Falta «" + etiqueta + "» en el Resumen")), 1);
    }

    private static BigDecimal num(Row fila, int columna) {
        return fila.getCellAsNumber(columna).orElseThrow(() -> new AssertionError("La columna " + columna + " no es un número"));
    }

    private static List<String> tablas(byte[] xlsx) throws Exception {
        java.nio.file.Path archivo = java.nio.file.Files.createTempFile("reporte", ".xlsx");
        List<String> nombres = new ArrayList<>();
        try {
            java.nio.file.Files.write(archivo, xlsx);
            try (ZipFile zip = new ZipFile(archivo.toFile())) {
                for (ZipEntry e : Collections.list(zip.entries())) {
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

    /** Un .xlsx con la misma cabecera que la plantilla descargada y las filas dadas. */
    private static byte[] llenar(List<String> cabecera, List<List<Object>> datos, String hoja) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Workbook wb = new Workbook(salida, "test", "1.0");
        Worksheet ws = wb.newWorksheet(hoja);
        for (int c = 0; c < cabecera.size(); c++) ws.value(0, c, cabecera.get(c));
        for (int r = 0; r < datos.size(); r++) {
            for (int c = 0; c < datos.get(r).size(); c++) {
                Object v = datos.get(r).get(c);
                if (v instanceof Number n) ws.value(r + 1, c, n);
                else if (v != null) ws.value(r + 1, c, v.toString());
            }
        }
        wb.finish();
        return salida.toByteArray();
    }

    @Test
    @DisplayName("Plantilla de productos: se descarga, se llena y se sube como .xlsx; la planilla de cantidades también")
    void plantillasDelInventario() throws Exception {
        String admin = login("admin", "admin123");

        byte[] plantilla = descargar(admin, "/api/v1/productos/plantilla-importacion");
        List<String> cabecera = new ArrayList<>();
        try (ReadableWorkbook libro = abrir(plantilla)) {
            assertThat(libro.getSheets().map(Sheet::getName)).containsExactly("Productos", "Instrucciones");
            Row fila = filas(libro, "Productos").get(0);
            for (int c = 0; c < fila.getCellCount(); c++) cabecera.add(fila.getCellText(c));
            assertThat(cabecera).startsWith("Código *", "Nombre *", "Tipo *");
        }
        // La lista desplegable de tipos y las validaciones numéricas viajan en la hoja
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("plantilla", ".xlsx");
        java.nio.file.Files.write(tmp, plantilla);
        try (ZipFile zip = new ZipFile(tmp.toFile())) {
            String hoja1 = new String(zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readAllBytes(), StandardCharsets.UTF_8);
            assertThat(hoja1).contains("dataValidation").contains("Llave virgen").contains("Repuesto de licuadora");
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }

        // Se llena como lo haría el dueño (con los títulos de la plantilla) y se sube tal cual, sin pasar por CSV
        byte[] lleno = llenar(cabecera, List.of(
            java.util.Arrays.asList("XL-LLAVE", "Llave Excel", "Llave virgen", "CV", 40, 10, 0.9, 1.5, 2.5, "XL1", null),
            java.util.Arrays.asList("XL-CAN", "Candado Excel", "Candado", null, 5, 2, "12,50", "20", null, null, null),
            java.util.Arrays.asList("XL-LIC", "Cuchilla Excel", "Repuesto de licuadora", "Oster", 3, 1, 14, 25, null, null, null)),
            "Productos");
        JsonNode revision = subir(admin, "/api/v1/productos/importar", lleno, "confirmar", "false");
        assertThat(revision.get("errores")).isEmpty();
        assertThat(revision.get("filasLeidas").asInt()).isEqualTo(3);
        assertThat(subir(admin, "/api/v1/productos/importar", lleno, "confirmar", "true").get("creados").asInt()).isEqualTo(3);
        JsonNode llave = llamar(admin, get("/api/v1/productos/buscar?q=XL-LLAVE"), null, status().isOk()).get(0);
        assertThat(llave.get("cantidadStock").asInt()).isEqualTo(40);
        assertThat(llave.get("precioDuplicado").decimalValue()).isEqualByComparingTo("2.5");
        assertThat(llave.get("llavePerfil").asString()).isEqualTo("XL1");
        assertThat(llamar(admin, get("/api/v1/productos/buscar?q=XL-CAN"), null, status().isOk()).get(0)
            .get("precioCompra").decimalValue()).as("la coma decimal se entiende").isEqualByComparingTo("12.5");

        // Un error en una fila no carga nada y dice la fila (la de Excel)
        byte[] malo = llenar(cabecera, List.of(
            java.util.Arrays.asList("XL-OK", "Bien", "Candado", null, 1, 1, 1, 2, null, null, null),
            java.util.Arrays.asList("XL-MAL", "Mal", "Cosa rara", null, 1, 1, 1, 2, null, null, null)), "Productos");
        JsonNode erroneo = subir(admin, "/api/v1/productos/importar", malo, "confirmar", "true");
        assertThat(erroneo.get("creados").asInt()).isZero();
        assertThat(erroneo.get("errores").get(0).get("fila").asInt()).isEqualTo(3);

        // Planilla de cantidades: se descarga con el inventario y la columna Cantidad lista para escribir
        byte[] planilla = descargar(admin, "/api/v1/productos/planilla", "formato", "xlsx");
        List<String> cab = new ArrayList<>();
        try (ReadableWorkbook libro = abrir(planilla)) {
            List<Row> inventario = filas(libro, "Inventario");
            Row titulos = inventario.get(0);
            for (int c = 0; c < titulos.getCellCount(); c++) cab.add(titulos.getCellText(c));
            assertThat(cab).containsExactly("Código", "Nombre", "Tipo", "Marca", "Stock actual", "Cantidad",
                "Precio compra", "Precio venta", "Precio duplicado");
            Row fila = inventario.stream().filter(r -> "XL-LLAVE".equals(r.getCellText(0))).findFirst().orElseThrow();
            assertThat(num(fila, 4)).isEqualByComparingTo("40");
            assertThat(fila.getCellAsString(5).orElse("")).as("cantidad vacía").isEmpty();
        }
        assertThat(tablas(planilla)).contains("Inventario");

        // …y se sube de vuelta contando: la llave pasa de 40 a 55 y el precio de venta cambia
        byte[] contada = llenar(cab, List.of(java.util.Arrays.asList("XL-LLAVE", null, null, null, null, 55, null, 1.8, null)),
            "Inventario");
        JsonNode plan = subir(admin, "/api/v1/productos/planilla", contada, "modo", "CONTEO", "confirmar", "true");
        assertThat(plan.get("errores")).isEmpty();
        JsonNode despues = llamar(admin, get("/api/v1/productos/buscar?q=XL-LLAVE"), null, status().isOk()).get(0);
        assertThat(despues.get("cantidadStock").asInt()).isEqualTo(55);
        assertThat(despues.get("precioVenta").decimalValue()).isEqualByComparingTo("1.8");

        // El empleado no descarga la plantilla de carga
        llamar(admin, post("/api/v1/usuarios"), """
            {"username":"empleado_xl","password":"clave123","nombre":"Empleado","role":"ROLE_EMPLOYEE"}
            """, status().isCreated());
        mockMvc.perform(get("/api/v1/productos/plantilla-importacion").header("Authorization", login("empleado_xl", "clave123")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Los cuatro reportes salen con los números del periodo, tablas con nombre y totales que siguen al filtro")
    void reportesEnExcel() throws Exception {
        String admin = login("admin", "admin123");
        long cajaId = llamar(admin, post("/api/v1/caja/abrir"), "{\"montoApertura\": 50}", status().isCreated()).get("id").asLong();
        long llave = llamar(admin, post("/api/v1/productos"), """
            {"codigo":"RP-LLAVE","nombre":"Llave reporte","tipoProducto":"LLAVE_VIRGEN","marca":"CV",
             "cantidadStock":3,"stockMinimo":10,"precioCompra":0.9,"precioVenta":1.5,"precioDuplicado":2.5}
            """, status().isCreated()).get("id").asLong();
        llamar(admin, post("/api/v1/ventas"), """
            {"metodoPago":"EFECTIVO","items":[{"productoId":%d,"cantidad":2,"duplicado":true},
              {"cantidad":1,"descripcion":"Abrir candado","precio":10}]}
            """.formatted(llave), status().isCreated());

        String ayer = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        JsonNode anteriores = subir(admin, "/api/v1/analisis/ventas-anteriores",
            ("fecha;producto;codigo;cantidad;precio;metodo_pago\n" + ayer + ";Llave reporte;RP-LLAVE;10;2,50;efectivo\n"
                + ayer + ";Candado viejo;;1;30;yape\n").getBytes(StandardCharsets.UTF_8), "confirmar", "true");
        assertThat(anteriores.get("cargadas").asInt()).isEqualTo(2);

        // Taller, domicilio y compras, directo en la base (los servicios solo leen)
        Long usuario = jdbc.queryForObject("SELECT id FROM usuarios WHERE username = 'admin'", Long.class);
        Long cliente = jdbc.queryForObject("INSERT INTO clientes (nombre, telefono) VALUES ('Rosa Quispe', '999111222') RETURNING id", Long.class);
        Long orden = jdbc.queryForObject("""
            INSERT INTO ordenes_trabajo (numero_orden, cliente_id, producto_recibido, problema, precio_total, monto_pagado,
                                         estado, usuario_id)
            VALUES ('OT-TEST-1', ?, 'Licuadora Oster', 'No gira', 50, 20, 'LISTO', ?) RETURNING id
            """, Long.class, cliente, usuario);
        jdbc.update("INSERT INTO pagos_orden (orden_trabajo_id, caja_diaria_id, monto, tipo_pago, metodo_pago, usuario_id) "
            + "VALUES (?, ?, 20, 'ADELANTO', 'YAPE', ?)", orden, cajaId, usuario);
        jdbc.update("""
            INSERT INTO servicios_domicilio (cliente_id, direccion, telefono, fecha, hora, tipo_servicio, estado, usuario_id,
                                             monto_cobrado, metodo_pago, caja_diaria_id, realizado_por, fecha_realizado)
            VALUES (?, 'Av. Test 123', '999111222', CURRENT_DATE, '10:00', 'Cambio de chapa', 'REALIZADO', ?, 80, 'EFECTIVO', ?, ?, now())
            """, cliente, usuario, cajaId, usuario);
        Long proveedor = jdbc.queryForObject("INSERT INTO proveedores (nombre) VALUES ('Distribuidora Test') RETURNING id", Long.class);
        jdbc.update("INSERT INTO compras (proveedor_id, producto_id, cantidad, precio_unitario, caja_diaria_id, usuario_id) "
            + "VALUES (?, ?, 100, 0.9, ?, ?)", proveedor, llave, cajaId, usuario);

        String desde = LocalDate.now().minusDays(2).toString();
        String hasta = LocalDate.now().plusDays(1).toString();

        // ---- Resumen del periodo
        byte[] resumen = descargar(admin, "/api/v1/analisis/reportes/resumen", "desde", desde, "hasta", hasta);
        try (ReadableWorkbook libro = abrir(resumen)) {
            assertThat(libro.getSheets().map(Sheet::getName)).containsExactly("Resumen", "Por día",
                "Por método de pago", "Productos", "Por tipo", "Clientes");
            assertThat(filas(libro, "Resumen").get(0).getCellText(0)).contains("Cerrajería Barrera").contains("Resumen del periodo");
            assertThat(kpi(libro, "Ventas del sistema")).isEqualByComparingTo("15");
            assertThat(kpi(libro, "Ventas anteriores")).isEqualByComparingTo("55"); // 25 + 30
            assertThat(kpi(libro, "Cobrado en el taller (neto)")).isEqualByComparingTo("20");
            assertThat(kpi(libro, "Cobrado en domicilios")).isEqualByComparingTo("80");
            assertThat(kpi(libro, "Comprado a distribuidores")).isEqualByComparingTo("90");
            Row totales = filas(libro, "Resumen").stream().filter(r -> "Ventas totales".equals(r.getCellText(0))).findFirst().orElseThrow();
            assertThat(totales.getCell(1).getFormula()).as("derivado con fórmula").isEqualTo("B5+B6");

            List<Row> metodos = filas(libro, "Por método de pago");
            assertThat(metodos.stream().map(r -> r.getCellText(0))).contains("Efectivo", "Yape");
            // Los totales de arriba siguen al filtro: son SUBTOTAL, no un número fijo
            Row total = metodos.get(3);
            assertThat(total.getCellText(0)).isEqualTo("TOTAL");
            assertThat(total.getCell(4).getFormula()).startsWith("SUBTOTAL(109,E");
            // Productos: la llave de la venta del sistema y las anteriores suman 12 unidades (2 + 10)
            Row productoFila = filas(libro, "Productos").stream().filter(r -> "Llave reporte".equals(r.getCellText(0))).findFirst().orElseThrow();
            assertThat(num(productoFila, 3)).isEqualByComparingTo("12");
        }
        assertThat(tablas(resumen)).contains("PorDia", "PorMetodo", "ProductosVendidos", "PorTipo", "Clientes");

        // ---- Cierre de caja
        byte[] caja = descargar(admin, "/api/v1/analisis/reportes/caja", "desde", desde, "hasta", hasta);
        try (ReadableWorkbook libro = abrir(caja)) {
            assertThat(libro.getSheets().map(Sheet::getName)).containsExactly("Resumen", "Cajas", "Movimientos");
            assertThat(kpi(libro, "Cajas abiertas en el periodo")).isEqualByComparingTo("1");
            assertThat(kpi(libro, "Todavía sin cerrar")).isEqualByComparingTo("1");
            assertThat(kpi(libro, "Monto inicial (suma de aperturas)")).isEqualByComparingTo("50");
            List<Row> cajas = filas(libro, "Cajas");
            assertThat(cajas.get(4).getCellText(11)).isEqualTo("Efectivo esperado");
            assertThat(cajas).hasSize(6); // título, aviso, totales y cabecera + 1 caja
        }

        // ---- Compras y lista de compras
        byte[] compras = descargar(admin, "/api/v1/analisis/reportes/compras", "desde", desde, "hasta", hasta);
        try (ReadableWorkbook libro = abrir(compras)) {
            assertThat(libro.getSheets().map(Sheet::getName)).containsExactly("Resumen", "Lista de compras", "Compras", "Por distribuidor");
            assertThat(kpi(libro, "Total comprado")).isEqualByComparingTo("90");
            // La llave quedó con 1 (3 menos los 2 duplicados vendidos), bajo el mínimo de 10
            List<Row> lista = filas(libro, "Lista de compras");
            Row producto = lista.stream().filter(r -> "RP-LLAVE".equals(r.getCellText(0))).findFirst().orElseThrow();
            assertThat(num(producto, 7)).as("A comprar = el doble del mínimo menos el stock").isEqualByComparingTo("19");
            assertThat(producto.getCell(6).getFormula()).startsWith("MAX(0,2*E");
            assertThat(producto.getCell(9).getFormula()).startsWith("H");
        }
        assertThat(tablas(compras)).contains("ListaCompras", "Compras", "PorDistribuidor");

        // ---- Taller y domicilio
        byte[] taller = descargar(admin, "/api/v1/analisis/reportes/taller", "desde", desde, "hasta", hasta);
        try (ReadableWorkbook libro = abrir(taller)) {
            assertThat(libro.getSheets().map(Sheet::getName)).containsExactly("Resumen", "Taller", "Domicilio", "Por técnico");
            assertThat(kpi(libro, "Órdenes recibidas")).isEqualByComparingTo("1");
            assertThat(kpi(libro, "Saldo por cobrar hoy")).isEqualByComparingTo("30");
            assertThat(kpi(libro, "Servicios realizados")).isEqualByComparingTo("1");
            Row orden1 = filas(libro, "Taller").stream().filter(r -> "OT-TEST-1".equals(r.getCellText(0))).findFirst().orElseThrow();
            assertThat(orden1.getCellText(3)).isEqualTo("Listo");
            assertThat(num(orden1, 10)).isEqualByComparingTo("30");
            Row serv = filas(libro, "Domicilio").stream().filter(r -> "Cambio de chapa".equals(r.getCellText(2))).findFirst().orElseThrow();
            assertThat(serv.getCellText(1)).isEqualTo("10:00");
        }

        // Un periodo al revés da un error claro, y el empleado no los descarga
        mockMvc.perform(get("/api/v1/analisis/reportes/resumen").param("desde", hasta).param("hasta", desde)
                .header("Authorization", admin)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/analisis/reportes/nada").param("desde", desde).param("hasta", hasta)
                .header("Authorization", admin)).andExpect(status().isBadRequest());
    }
}
