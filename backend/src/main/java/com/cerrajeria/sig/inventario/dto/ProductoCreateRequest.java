package com.cerrajeria.sig.inventario.dto;

import com.cerrajeria.sig.inventario.TipoProducto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductoCreateRequest(
    @NotBlank(message = "El código es obligatorio") String codigo,
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    @NotNull(message = "El tipo de producto es obligatorio") TipoProducto tipoProducto,
    String marca,
    @NotNull @PositiveOrZero(message = "La cantidad inicial no puede ser negativa") Integer cantidadStock,
    @PositiveOrZero(message = "El stock mínimo no puede ser negativo") Integer stockMinimo,
    @NotNull @PositiveOrZero(message = "El precio de compra no puede ser negativo") BigDecimal precioCompra,
    @NotNull @PositiveOrZero(message = "El precio de venta no puede ser negativo") BigDecimal precioVenta,
    String imagenUrl,
    String llavePerfil,
    String llaveColor,
    String llaveDisenoTematica,
    String productoModelo,
    String productoTamano,
    String productoMaterial,
    /** Solo llaves: lo que se cobra por el duplicado. Vacio: no aparece en "Duplicado de llave". */
    @PositiveOrZero(message = "El precio del duplicado no puede ser negativo") BigDecimal precioDuplicado
) {
}
