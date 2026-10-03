package com.cerrajeria.sig.common.exception;

/** 409 - Venta o salida que excede el stock disponible de un producto. */
public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String message) {
        super(message);
    }
}
