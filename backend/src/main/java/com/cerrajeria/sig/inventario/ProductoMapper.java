package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.inventario.dto.ProductoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "stockBajo", expression = "java(producto.tieneBajoStock())")
    ProductoResponse toResponse(Producto producto);

    List<ProductoResponse> toResponseList(List<Producto> productos);
}
