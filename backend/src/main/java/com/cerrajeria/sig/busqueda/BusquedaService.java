package com.cerrajeria.sig.busqueda;

import org.springframework.transaction.annotation.Transactional;
import com.cerrajeria.sig.clientes.ClienteRepository;
import com.cerrajeria.sig.clientes.ClienteMapper;
import com.cerrajeria.sig.inventario.ProductoRepository;
import com.cerrajeria.sig.inventario.ProductoMapper;
import com.cerrajeria.sig.ordenes.OrdenTrabajo;
import com.cerrajeria.sig.ordenes.OrdenTrabajoRepository;
import com.cerrajeria.sig.ordenes.dto.OrdenTrabajoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Buscador global instantaneo (RF-46): cruza productos, clientes y ordenes. */
@Service
@RequiredArgsConstructor
public class BusquedaService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final OrdenTrabajoRepository ordenTrabajoRepository;

    @Transactional(readOnly = true)
    public ResultadoBusquedaResponse buscar(String termino) {
        var productos = productoMapper.toResponseList(productoRepository.buscar(termino));
        var clientes = clienteMapper.toResponseList(clienteRepository.buscar(termino));
        var ordenes = ordenTrabajoRepository.findAllByOrderByFechaIngresoDesc().stream()
            .filter(o -> coincide(o, termino))
            .limit(20)
            .map(this::toOrdenResponse)
            .toList();

        return new ResultadoBusquedaResponse(productos, clientes, ordenes);
    }

    private boolean coincide(OrdenTrabajo o, String termino) {
        String t = termino.toLowerCase();
        return o.getNumeroOrden().toLowerCase().contains(t)
            || o.getCliente().getNombre().toLowerCase().contains(t)
            || o.getProductoRecibido().toLowerCase().contains(t);
    }

    private OrdenTrabajoResponse toOrdenResponse(OrdenTrabajo o) {
        return new OrdenTrabajoResponse(
            o.getId(), o.getNumeroOrden(), o.getCliente().getNombre(), o.getProductoRecibido(),
            o.getProblema(), o.getDiagnostico(), o.getPrecioTotal(), o.getMontoPagado(),
            o.getSaldoPendiente(), o.getEstado().name(), o.getFechaIngreso(), o.getFechaPrometida(),
            o.getFechaEntrega(), o.getObservaciones(), o.getMotivoCancelacion(), o.isGarantia(),
            o.getOrdenOrigen() == null ? null : o.getOrdenOrigen().getNumeroOrden());
    }
}
