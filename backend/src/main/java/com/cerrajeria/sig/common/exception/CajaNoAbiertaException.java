package com.cerrajeria.sig.common.exception;

/** 409 - Regla de negocio 1: operacion monetaria sin caja diaria ABIERTA. */
public class CajaNoAbiertaException extends RuntimeException {
    public CajaNoAbiertaException(String message) {
        super(message);
    }
}
