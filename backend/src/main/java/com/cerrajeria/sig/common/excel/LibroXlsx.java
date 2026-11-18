package com.cerrajeria.sig.common.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Deja un .xlsx como un ZIP estandar. La libreria que escribe los libros arma cada entrada sin
 * los tamanos en su cabecera local: Excel lo tolera (lee el directorio del final), pero
 * LibreOffice, Google Sheets en algunos casos y los lectores de ZIP en streaming lo rechazan
 * ("invalid entry size"). Aca se vuelve a empaquetar con todos los datos en su sitio, y con
 * [Content_Types].xml primero, como lo escribe Excel. El contenido no cambia ni una letra.
 */
public final class LibroXlsx {

    private LibroXlsx() {
    }

    public static byte[] normalizar(byte[] xlsx) {
        Path temporal = null;
        try {
            temporal = Files.createTempFile("libro", ".xlsx");
            Files.write(temporal, xlsx);
            // ZipFile lee por el directorio central, que es lo unico confiable en el original
            try (ZipFile origen = new ZipFile(temporal.toFile())) {
                List<? extends ZipEntry> entradas = Collections.list(origen.entries()).stream()
                    .sorted(Comparator.comparingInt((ZipEntry e) -> prioridad(e.getName()))).toList();
                ByteArrayOutputStream salida = new ByteArrayOutputStream(xlsx.length);
                try (ZipOutputStream zip = new ZipOutputStream(salida)) {
                    for (ZipEntry entrada : entradas) {
                        copiar(zip, entrada.getName(), origen.getInputStream(entrada).readAllBytes());
                    }
                }
                return salida.toByteArray();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo dejar el Excel como un ZIP estándar", ex);
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ignorada) {
                    // un temporal que no se pudo borrar no cambia el resultado
                }
            }
        }
    }

    private static int prioridad(String nombre) {
        return switch (nombre) {
            case "[Content_Types].xml" -> 0;
            case "_rels/.rels" -> 1;
            default -> 2; // el resto conserva su orden (el sort es estable)
        };
    }

    /**
     * Con tamano, tamano comprimido y CRC conocidos de antemano, ZipOutputStream no necesita el
     * "data descriptor": los datos quedan completos en la cabecera local.
     */
    private static void copiar(ZipOutputStream zip, String nombre, byte[] datos) throws IOException {
        CRC32 crc = new CRC32();
        crc.update(datos);
        // Misma configuracion que usa ZipOutputStream por dentro, para que el tamano comprimido coincida
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        long comprimido = 0;
        try {
            deflater.setInput(datos);
            deflater.finish();
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                comprimido += deflater.deflate(buffer);
            }
        } finally {
            deflater.end();
        }
        ZipEntry entrada = new ZipEntry(nombre);
        entrada.setMethod(ZipEntry.DEFLATED);
        entrada.setSize(datos.length);
        entrada.setCompressedSize(comprimido);
        entrada.setCrc(crc.getValue());
        zip.putNextEntry(entrada);
        zip.write(datos);
        zip.closeEntry();
    }
}
