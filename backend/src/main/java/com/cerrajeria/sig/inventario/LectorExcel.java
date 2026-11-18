package com.cerrajeria.sig.inventario;

import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lee un libro .xlsx con la misma forma que {@link LectorCsv}: una lista de
 * filas, cada una una lista de celdas de texto. Asi los importadores validan
 * igual un CSV que un Excel.
 *
 * Toma la primera hoja que no sea de instrucciones. Las filas vacias se
 * conservan (como filas vacias) para que el numero de fila de un error sea el
 * que se ve en Excel.
 */
public final class LectorExcel {

    private static final Set<String> HOJAS_DE_AYUDA = Set.of("instrucciones", "leeme", "léeme", "ayuda", "listas");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT);
    private static final int MAX_FILAS = 100_000;

    private LectorExcel() {
    }

    static List<List<String>> leer(byte[] bytes) {
        // withCellFormat: sin eso no sabe que una celda con formato de fecha es una fecha
        try (ReadableWorkbook libro = new ReadableWorkbook(new ByteArrayInputStream(bytes), new ReadingOptions(true, false))) {
            Sheet hoja = libro.getSheets()
                .filter(h -> !HOJAS_DE_AYUDA.contains(h.getName().trim().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El libro de Excel no tiene ninguna hoja con datos"));
            List<List<String>> registros = new ArrayList<>();
            for (Row fila : hoja.read()) {
                if (fila.getRowNum() > MAX_FILAS) {
                    throw new IllegalArgumentException("El archivo tiene demasiadas filas");
                }
                while (registros.size() < fila.getRowNum() - 1) {
                    registros.add(List.of()); // fila vacia que Excel no guardo
                }
                List<String> celdas = new ArrayList<>();
                for (int c = 0; c < fila.getCellCount(); c++) {
                    celdas.add(texto(fila.getCell(c)));
                }
                registros.add(celdas);
            }
            return registros;
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof IllegalArgumentException iae) {
                throw iae;
            }
            throw new IllegalArgumentException(
                "No se pudo leer el libro de Excel. Guárdalo de nuevo como .xlsx (o como CSV UTF-8) y vuelve a subirlo.", ex);
        }
    }

    /** "dd/mm/yyyy", "yyyy-mm-dd hh:mm"…: letras de fecha y no solo de numero ("0.00", "#,##0"). */
    private static boolean esFormatoDeFecha(String formato) {
        if (formato == null) {
            return false;
        }
        String f = formato.toLowerCase(Locale.ROOT).replaceAll("\\[[^\\]]*]|\"[^\"]*\"", "");
        return f.matches(".*[dmyh].*") && !f.contains("general");
    }

    private static String texto(Cell celda) {
        if (celda == null || celda.getType() == CellType.EMPTY) {
            return "";
        }
        Object valor = celda.getValue();
        if (valor == null) {
            return "";
        }
        if (celda.getType() == CellType.NUMBER && esFormatoDeFecha(celda.getDataFormatString())) {
            valor = celda.asDate(); // por si el lector la dejo como numero de serie
        }
        if (valor instanceof LocalDateTime fecha) {
            return fecha.toLocalTime().toSecondOfDay() == 0 ? FECHA.format(fecha) : FECHA.format(fecha) + " " + fecha.toLocalTime();
        }
        if (valor instanceof BigDecimal numero) {
            return numero.stripTrailingZeros().toPlainString();
        }
        if (valor instanceof Number numero) {
            return new BigDecimal(numero.toString()).stripTrailingZeros().toPlainString();
        }
        if (valor instanceof Boolean si) {
            return si ? "Sí" : "No";
        }
        return valor.toString().trim();
    }
}
