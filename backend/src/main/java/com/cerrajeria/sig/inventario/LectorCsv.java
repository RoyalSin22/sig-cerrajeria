package com.cerrajeria.sig.inventario;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lector de CSV para la importacion de inventario. Pensado para el archivo que
 * produce Excel con "Guardar como → CSV", que cambia segun la PC:
 * <ul>
 *   <li>Codificacion: "CSV UTF-8" (con BOM) o "CSV (delimitado por comas)",
 *       que en Windows en espanol se guarda en Windows-1252 (las tildes y la
 *       ñ se romperian si se leyera como UTF-8).</li>
 *   <li>Separador: coma o punto y coma, segun la configuracion regional.
 *       Se respeta la linea "sep=;" que Excel entiende, si viene.</li>
 * </ul>
 * Soporta comillas dobles (campos con el separador o saltos de linea adentro,
 * y "" como comilla escapada), segun RFC 4180.
 */
final class LectorCsv {

    private LectorCsv() {
    }

    /** .xlsx/.ods son ZIP: se reconocen por la firma "PK" del inicio. */
    static void rechazarLibroExcel(byte[] archivo) {
        if (archivo.length >= 2 && archivo[0] == 'P' && archivo[1] == 'K') {
            throw new IllegalArgumentException(
                "Es un libro de Excel, no un CSV. En Excel usa Archivo → Guardar como → \"CSV UTF-8\" y sube ese archivo.");
        }
    }

    /** Cada registro es una lista de celdas, sin la linea "sep=" si existia. */
    static List<List<String>> leer(byte[] bytes) {
        String texto = decodificar(bytes);
        char separador;
        int inicio = 0;
        String primeraLinea = primeraLinea(texto);
        if (primeraLinea.regionMatches(true, 0, "sep=", 0, 4) && primeraLinea.length() >= 5) {
            separador = primeraLinea.charAt(4);
            inicio = Math.min(texto.length(), primeraLinea.length() + longitudSaltoDeLinea(texto, primeraLinea.length()));
        } else {
            separador = detectarSeparador(primeraLinea);
        }
        return parsear(texto.substring(inicio), separador);
    }

    private static String decodificar(byte[] bytes) {
        int desde = 0;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            desde = 3; // BOM de "CSV UTF-8" de Excel
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes, desde, bytes.length - desde);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(buffer)
                .toString();
        } catch (CharacterCodingException ex) {
            // No es UTF-8 valido: es el CSV "clasico" de Excel en Windows
            return new String(bytes, desde, bytes.length - desde, Charset.forName("windows-1252"));
        }
    }

    private static String primeraLinea(String texto) {
        int fin = 0;
        while (fin < texto.length() && texto.charAt(fin) != '\n' && texto.charAt(fin) != '\r') {
            fin++;
        }
        return texto.substring(0, fin);
    }

    private static int longitudSaltoDeLinea(String texto, int posicion) {
        if (posicion >= texto.length()) return 0;
        if (texto.charAt(posicion) == '\r' && posicion + 1 < texto.length() && texto.charAt(posicion + 1) == '\n') {
            return 2;
        }
        return 1;
    }

    /** El separador es el que mas aparece en la cabecera (fuera de comillas). */
    private static char detectarSeparador(String cabecera) {
        int comas = 0;
        int puntoYComas = 0;
        int tabs = 0;
        boolean enComillas = false;
        for (char c : cabecera.toCharArray()) {
            if (c == '"') enComillas = !enComillas;
            else if (!enComillas && c == ',') comas++;
            else if (!enComillas && c == ';') puntoYComas++;
            else if (!enComillas && c == '\t') tabs++;
        }
        if (puntoYComas > comas && puntoYComas >= tabs) return ';';
        if (tabs > comas && tabs > puntoYComas) return '\t';
        return ',';
    }

    private static List<List<String>> parsear(String texto, char separador) {
        List<List<String>> registros = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        StringBuilder celda = new StringBuilder();
        boolean enComillas = false;
        int i = 0;
        while (i < texto.length()) {
            char c = texto.charAt(i);
            if (enComillas) {
                if (c == '"') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                        celda.append('"');
                        i++;
                    } else {
                        enComillas = false;
                    }
                } else {
                    celda.append(c);
                }
            } else if (c == '"') {
                enComillas = true;
            } else if (c == separador) {
                actual.add(celda.toString());
                celda.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                actual.add(celda.toString());
                celda.setLength(0);
                registros.add(actual);
                actual = new ArrayList<>();
            } else {
                celda.append(c);
            }
            i++;
        }
        if (celda.length() > 0 || !actual.isEmpty()) {
            actual.add(celda.toString());
            registros.add(actual);
        }
        return registros;
    }
}
