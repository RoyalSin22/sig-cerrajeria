package com.cerrajeria.sig.inventario.dto;

import com.cerrajeria.sig.inventario.dto.ImportacionResultado.ErrorFila;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de leer (y, si se confirmo y no hubo errores, aplicar) la planilla
 * de cantidades y precios. Es todo o nada, como la importacion de productos.
 *
 * @param filasLeidas  filas con la cantidad o algun precio lleno
 * @param actualizados productos que cambiaron; 0 si fue solo una revision o hubo errores
 * @param sinCambio    filas cuyos valores ya eran los actuales
 * @param cambios      lo que va a cambiar (o cambio), en el orden del archivo
 * @param errores      problemas por fila; vacio si la planilla se puede aplicar
 */
public record ResultadoPlanilla(
    int filasLeidas,
    int actualizados,
    int sinCambio,
    List<CambioFila> cambios,
    List<ErrorFila> errores
) {

    /** {@code stockAnterior}/{@code stockNuevo} son null si la fila no cambia el stock. */
    public record CambioFila(int fila, String codigo, String nombre, Integer stockAnterior, Integer stockNuevo,
                             List<CambioPrecio> precios) {
    }

    /** {@code precio}: "compra", "venta" o "duplicado". {@code anterior} es null si no tenia. */
    public record CambioPrecio(String precio, BigDecimal anterior, BigDecimal nuevo) {
    }
}
