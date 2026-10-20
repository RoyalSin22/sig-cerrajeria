package com.cerrajeria.sig.inventario.dto;

import java.math.BigDecimal;

public record ProductoResponse(
    Long id,
    String codigo,
    String nombre,
    String tipoProducto,
    String marca,
    Integer cantidadStock,
    Integer stockMinimo,
    BigDecimal precioCompra,
    BigDecimal precioVenta,
    BigDecimal precioDuplicado,
    String imagenUrl,
    String llavePerfil,
    String llaveColor,
    String llaveDisenoTematica,
    String productoModelo,
    String productoTamano,
    String productoMaterial,
    Boolean activo,
    Boolean stockBajo
) {
}
