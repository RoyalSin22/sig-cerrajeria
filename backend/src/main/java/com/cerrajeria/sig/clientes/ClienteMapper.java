package com.cerrajeria.sig.clientes;

import com.cerrajeria.sig.clientes.dto.ClienteResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    ClienteResponse toResponse(Cliente cliente);
    List<ClienteResponse> toResponseList(List<Cliente> clientes);
}
