package com.cerrajeria.sig.domicilio;

import com.cerrajeria.sig.domicilio.dto.ServicioDomicilioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServicioDomicilioMapper {

    @Mapping(target = "cliente", source = "cliente.nombre")
    @Mapping(target = "realizadoPor", source = "realizadoPor.nombre")
    ServicioDomicilioResponse toResponse(ServicioDomicilio servicio);

    List<ServicioDomicilioResponse> toResponseList(List<ServicioDomicilio> servicios);
}
