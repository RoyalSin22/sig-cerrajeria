package com.cerrajeria.sig.caja;

import com.cerrajeria.sig.caja.dto.CajaDiariaResponse;
import com.cerrajeria.sig.caja.dto.MovimientoCajaResponse;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CajaMapper {

    @Mapping(target = "usuario", source = "usuario.nombre")
    @Mapping(target = "saldoEsperado", expression = "java(caja.saldoEsperado())")
    CajaDiariaResponse toResponse(CajaDiaria caja);

    @Mapping(target = "usuario", source = "usuario.nombre")
    MovimientoCajaResponse toResponse(MovimientoCaja movimiento);

    List<MovimientoCajaResponse> toResponseList(List<MovimientoCaja> movimientos);
}
