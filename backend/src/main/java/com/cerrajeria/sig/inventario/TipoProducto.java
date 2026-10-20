package com.cerrajeria.sig.inventario;

/** Tipos de producto del catalogo (Fase 6, CHECK de la tabla productos). */
public enum TipoProducto {
    LLAVE_VIRGEN,
    LLAVE_COLOR,
    LLAVE_DISENO,
    CHAPA,
    CILINDRO,
    CANDADO,
    LLAVERO,
    ARGOLLA,
    ACCESORIO;

    /** Las llaves se pueden vender como duplicado, con su propio precio. */
    public boolean esLlave() {
        return this == LLAVE_VIRGEN || this == LLAVE_COLOR || this == LLAVE_DISENO;
    }
}
