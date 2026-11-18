package com.cerrajeria.sig.inventario;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LectorExcel: un .xlsx se lee igual que un CSV")
class LectorExcelTest {

    private static byte[] libro(boolean ayudaPrimero) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Workbook wb = new Workbook(salida, "test", "1.0");
        if (ayudaPrimero) {
            wb.newWorksheet("Instrucciones").value(0, 0, "Esto no son datos");
        }
        Worksheet datos = wb.newWorksheet("Productos");
        datos.value(0, 0, "Código");
        datos.value(0, 1, "Precio");
        datos.value(0, 2, "Fecha");
        datos.value(0, 3, "Cantidad");
        datos.value(1, 0, "KW1-CV");
        datos.value(1, 1, 2.5);
        datos.value(1, 2, LocalDate.of(2025, 3, 15));
        datos.style(1, 2).format("dd/mm/yyyy").set(); // como lo guarda Excel: una fecha siempre trae su formato
        datos.value(1, 3, 10);
        // la fila 3 (indice 2) queda vacia: Excel no la guarda
        datos.value(3, 0, "TRX-CV");
        datos.value(3, 3, 3.0);
        wb.finish();
        return salida.toByteArray();
    }

    @Test
    @DisplayName("lee texto, números y fechas; conserva el número de fila aunque haya filas vacías")
    void leeTextoNumerosYFechas() throws Exception {
        List<List<String>> filas = LectorCsv.leer(libro(false));

        assertThat(filas).hasSize(4);
        assertThat(filas.get(0)).containsExactly("Código", "Precio", "Fecha", "Cantidad");
        assertThat(filas.get(1)).containsExactly("KW1-CV", "2.5", "15/03/2025", "10");
        assertThat(filas.get(2)).as("la fila vacía sigue ocupando su lugar").allMatch(String::isEmpty);
        assertThat(filas.get(3).get(0)).isEqualTo("TRX-CV");
        assertThat(filas.get(3).get(3)).as("3.0 sin decimales de más").isEqualTo("3");
    }

    @Test
    @DisplayName("salta la hoja de instrucciones y toma la de datos")
    void saltaLaHojaDeInstrucciones() throws Exception {
        assertThat(LectorCsv.leer(libro(true)).get(0)).startsWith("Código");
    }

    @Test
    @DisplayName("un archivo que parece Excel pero no lo es da un mensaje claro")
    void archivoRoto() {
        byte[] roto = {'P', 'K', 1, 2, 3, 4};
        assertThatThrownBy(() -> LectorCsv.leer(roto))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("No se pudo leer el libro de Excel");
    }

    @Test
    @DisplayName("un CSV sigue leyéndose como antes")
    void csvSigueIgual() {
        List<List<String>> filas = LectorCsv.leer("codigo;nombre\r\nA-1;Llave\r\n".getBytes());
        assertThat(filas).containsExactly(List.of("codigo", "nombre"), List.of("A-1", "Llave"));
    }
}
