package com.cerrajeria.sig.common.excel;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LibroXlsx: el .xlsx sale como un ZIP estándar")
class LibroXlsxTest {

    private static byte[] libro() throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Workbook wb = new Workbook(salida, "test", "1.0");
        Worksheet ws = wb.newWorksheet("Datos");
        ws.value(0, 0, "Producto");
        ws.value(0, 1, "Monto");
        for (int i = 1; i <= 500; i++) {
            ws.value(i, 0, "Llave " + i);
            ws.value(i, 1, i * 1.5);
        }
        ws.range(0, 0, 500, 1).createTable("Producto", "Monto").setName("Datos").setDisplayName("Datos");
        wb.newWorksheet("Otra").value(0, 0, "hola");
        wb.finish();
        return salida.toByteArray();
    }

    /** Un lector en streaming (lo que usan LibreOffice y otras herramientas) valida cada cabecera local. */
    private static List<String> leerEstricto(byte[] zip) throws Exception {
        List<String> nombres = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                assertThat(in.readAllBytes()).as(e.getName()).isNotEmpty();
                nombres.add(e.getName());
            }
        }
        return nombres;
    }

    @Test
    @DisplayName("se lee de punta a punta en streaming, con [Content_Types].xml primero")
    void seLeeEnStreaming() throws Exception {
        byte[] normalizado = LibroXlsx.normalizar(libro());

        List<String> nombres = leerEstricto(normalizado);
        assertThat(nombres).startsWith("[Content_Types].xml", "_rels/.rels");
        assertThat(nombres).contains("xl/workbook.xml", "xl/tables/table1.xml");
    }

    @Test
    @DisplayName("el contenido no cambia: mismas hojas, mismas celdas")
    void elContenidoNoCambia() throws Exception {
        byte[] original = libro();
        byte[] normalizado = LibroXlsx.normalizar(original);

        try (ReadableWorkbook a = new ReadableWorkbook(new ByteArrayInputStream(original));
             ReadableWorkbook b = new ReadableWorkbook(new ByteArrayInputStream(normalizado))) {
            assertThat(b.getSheets().map(s -> s.getName())).containsExactly("Datos", "Otra");
            var filasA = a.getFirstSheet().read();
            var filasB = b.getFirstSheet().read();
            assertThat(filasB).hasSameSizeAs(filasA).hasSize(501);
            assertThat(filasB.get(500).getCellText(0)).isEqualTo("Llave 500");
            assertThat(filasB.get(500).getCellAsNumber(1).orElseThrow()).isEqualByComparingTo("750");
        }
    }
}
