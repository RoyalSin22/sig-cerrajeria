package com.cerrajeria.sig.common.exception;

/** 404 - Cliente, producto, orden u otro recurso inexistente. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
