package com.cerrajeria.sig.common.exception;

/** 409 - Regla de negocio 3: intento de marcar ENTREGADO con saldo > 0. */
public class SaldoPendienteException extends RuntimeException {
    public SaldoPendienteException(String message) {
        super(message);
    }
}
