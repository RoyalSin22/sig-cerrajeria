package com.cerrajeria.sig.inventario.dto;

import com.cerrajeria.sig.inventario.dto.ImportacionResultado.ErrorFila;

import java.util.List;

/**
 * Resultado de leer (y, si se confirmo y no hubo errores, aplicar) una
 * planilla de cantidades. Es todo o nada, como la importacion de productos.
 *
 * @param filasConCantidad filas con la columna "cantidad" llena
 * @param actualizados     productos cuyo stock cambio; 0 si fue solo una revision o hubo errores
 * @param sinCambio        filas de conteo cuyo numero ya era el stock actual
 * @param cambios          lo que va a cambiar (o cambio), en el orden del archivo
 * @param errores          problemas por fila; vacio si la planilla se puede aplicar
 */
public record ActualizacionStockResultado(
    int filasConCantidad,
    int actualizados,
    int sinCambio,
    List<CambioStock> cambios,
    List<ErrorFila> errores
) {

    public record CambioStock(int fila, String codigo, String nombre, int stockAnterior, int stockNuevo) {
    }
}
