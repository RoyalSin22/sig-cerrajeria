package com.cerrajeria.sig.analisis.dto;

import com.cerrajeria.sig.inventario.dto.ImportacionResultado.ErrorFila;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Revision (o carga, si se confirmo y no hubo errores) de un archivo de ventas
 * anteriores. Es todo o nada, como la importacion de productos.
 *
 * @param enlazadas     lineas que coincidieron con un producto del inventario (por codigo o nombre)
 * @param muestra       las primeras lineas leidas, para ver que se entendio bien el archivo
 */
public record ResultadoVentasAnteriores(
    int filasLeidas,
    int cargadas,
    int enlazadas,
    BigDecimal total,
    LocalDate desde,
    LocalDate hasta,
    List<Linea> muestra,
    List<ErrorFila> errores,
    List<String> columnasIgnoradas
) {
    public record Linea(int fila, LocalDate fecha, String producto, boolean enlazada, int cantidad,
                        BigDecimal precioUnitario, String metodoPago) {
    }
}
