package com.cerrajeria.sig.inventario.dto;

import com.cerrajeria.sig.inventario.TipoProducto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de leer (y, si se confirmo y no hubo errores, importar) un CSV de
 * productos. La importacion es todo o nada: con un solo error no se crea
 * ningun producto, para que la carga no quede a medias.
 *
 * @param filasLeidas        filas con datos (sin la cabecera ni filas vacias)
 * @param creados            productos creados; 0 si fue solo una revision o hubo errores
 * @param productos          filas validas, en el orden del archivo
 * @param errores            problemas por fila; vacio si el archivo se puede importar
 * @param columnasIgnoradas  columnas de la cabecera que no se reconocieron
 */
public record ImportacionResultado(
    int filasLeidas,
    int creados,
    List<FilaProducto> productos,
    List<ErrorFila> errores,
    List<String> columnasIgnoradas
) {

    /** {@code fila} es el numero de fila como se ve en Excel (la cabecera es la 1). */
    public record FilaProducto(int fila, String codigo, String nombre, TipoProducto tipoProducto,
                               int cantidadStock, BigDecimal precioVenta) {
    }

    /** {@code fila} 0 indica un problema del archivo entero (ej. falta una columna). */
    public record ErrorFila(int fila, String mensaje) {
    }
}
