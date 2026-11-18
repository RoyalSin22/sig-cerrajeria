package com.cerrajeria.sig.common.excel;

import org.dhatim.fastexcel.BorderStyle;
import org.dhatim.fastexcel.PaperSize;
import org.dhatim.fastexcel.Position;
import org.dhatim.fastexcel.Worksheet;

/**
 * Aspecto comun de todos los Excel que genera el sistema (plantillas y reportes):
 * los azules de la app, cabeceras legibles, montos en soles y hojas listas para
 * imprimir en A4. Solo colores y formatos; los datos los pone cada servicio.
 */
public final class EstiloExcel {

    public static final String AZUL = "0A3D73";       // cabecera principal (el azul de la app)
    public static final String AZUL_SUAVE = "5B7C99";  // cabecera de lo opcional
    public static final String CELESTE = "DCE8F5";     // fondo de totales
    public static final String GRIS = "F0F2F4";        // solo lectura
    public static final String AMARILLO = "FFF2CC";    // aqui se escribe
    public static final String NARANJA = "FDE8D7";     // atencion
    public static final String VERDE = "E2F3EA";       // bien
    public static final String TINTA = "1A1A1A";
    public static final String SUAVE = "5F6B76";
    public static final String LINEA = "C9D1D9";

    public static final String SOLES = "\"S/\" #,##0.00";
    public static final String ENTERO = "#,##0";
    public static final String FECHA = "dd/mm/yyyy";
    public static final String FECHA_HORA = "dd/mm/yyyy hh:mm";

    private EstiloExcel() {
    }

    /** Fila de titulos de una tabla: texto blanco en negrita sobre {@code fondo}. */
    public static void cabecera(Worksheet hoja, int fila, int desde, int hasta, String fondo) {
        hoja.range(fila, desde, fila, hasta).style()
            .bold().fontColor("FFFFFF").fillColor(fondo)
            .wrapText(true).verticalAlignment("center").horizontalAlignment("left")
            .borderStyle(BorderStyle.THIN).borderColor(LINEA).set();
        hoja.rowHeight(fila, 32);
    }

    public static void titulo(Worksheet hoja, int fila, String texto) {
        hoja.value(fila, 0, texto);
        hoja.style(fila, 0).bold().fontSize(18).fontColor(AZUL).set();
        hoja.rowHeight(fila, 28);
    }

    public static void subtitulo(Worksheet hoja, int fila, String texto) {
        hoja.value(fila, 0, texto);
        hoja.style(fila, 0).fontColor(SUAVE).fontSize(11).set();
    }

    /** Titulo de una seccion dentro de una hoja (por ejemplo "Por metodo de pago"). */
    public static void seccion(Worksheet hoja, int fila, String texto) {
        hoja.value(fila, 0, texto);
        hoja.style(fila, 0).bold().fontSize(13).fontColor(AZUL).set();
    }

    /** Bordes finos y suaves en un bloque de celdas. */
    public static void bordes(Worksheet hoja, int f1, int c1, int f2, int c2) {
        hoja.range(f1, c1, f2, c2).style().borderStyle(BorderStyle.THIN).borderColor(LINEA).set();
    }

    /** A4 de una pagina de ancho, con numero de pagina y titulos de tabla repetidos al imprimir. */
    public static void imprimible(Worksheet hoja, boolean horizontal, String pie) {
        hoja.paperSize(PaperSize.A4_PAPER);
        hoja.pageOrientation(horizontal ? "landscape" : "portrait");
        hoja.setFitToPage(true);
        hoja.fitToWidth((short) 1);
        hoja.fitToHeight((short) 0);
        hoja.footer(pie, Position.LEFT);
        hoja.footer("Página &P de &N", Position.RIGHT);
    }
}
