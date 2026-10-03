package com.cerrajeria.sig.busqueda;

import com.cerrajeria.sig.clientes.dto.ClienteResponse;
import com.cerrajeria.sig.inventario.dto.ProductoResponse;
import com.cerrajeria.sig.ordenes.dto.OrdenTrabajoResponse;

import java.util.List;

public record ResultadoBusquedaResponse(
    List<ProductoResponse> productos,
    List<ClienteResponse> clientes,
    List<OrdenTrabajoResponse> ordenes
) {
}
