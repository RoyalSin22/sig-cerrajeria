package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.inventario.dto.MovimientoInventarioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MovimientoInventarioMapper {

    @Mapping(target = "usuario", source = "usuario.nombre")
    MovimientoInventarioResponse toResponse(MovimientoInventario movimiento);

    List<MovimientoInventarioResponse> toResponseList(List<MovimientoInventario> movimientos);
}
