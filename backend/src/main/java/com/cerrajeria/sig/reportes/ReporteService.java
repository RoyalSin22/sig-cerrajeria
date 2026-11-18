package com.cerrajeria.sig.reportes;

import org.springframework.transaction.annotation.Transactional;
import com.cerrajeria.sig.inventario.ProductoRepository;
import com.cerrajeria.sig.ventas.DetalleVentaRepository;
import com.cerrajeria.sig.ventas.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;
    private final com.cerrajeria.sig.ordenes.PagoOrdenRepository pagoOrdenRepository;
    private final com.cerrajeria.sig.domicilio.ServicioDomicilioRepository servicioDomicilioRepository;
    private final com.cerrajeria.sig.ordenes.OrdenTrabajoRepository ordenTrabajoRepository;

    @Transactional(readOnly = true)
    public ReporteVentasResponse reporteVentas(LocalDate desde, LocalDate hasta) {
        var desdeDateTime = desde.atStartOfDay();
        var hastaDateTime = hasta.atTime(LocalTime.MAX);

        var total = ventaRepository.totalEntreFechas(desdeDateTime, hastaDateTime);
        var cantidad = ventaRepository.contarEntreFechas(desdeDateTime, hastaDateTime);
        var porMetodo = ventaRepository.totalPorMetodoPago(desdeDateTime, hastaDateTime).stream()
            .map(p -> new ReporteVentasResponse.VentaPorMetodoPago(p.getMetodoPago().name(), p.getTotal()))
            .toList();

        var taller = pagoOrdenRepository.ingresosNetosEntre(desdeDateTime, hastaDateTime);
        var domicilio = servicioDomicilioRepository.cobradoEntre(desdeDateTime, hastaDateTime);
        var servicios = servicioDomicilioRepository.countByEstadoAndFechaRealizadoBetween(
            com.cerrajeria.sig.domicilio.EstadoServicio.REALIZADO, desdeDateTime, hastaDateTime);

        return new ReporteVentasResponse(total, cantidad, porMetodo, taller, domicilio, servicios,
            total.add(taller).add(domicilio));
    }

    @Transactional(readOnly = true)
    public List<ProductoMasVendidoResponse> productosMasVendidos(LocalDate desde, LocalDate hasta) {
        var desdeDateTime = desde.atStartOfDay();
        var hastaDateTime = hasta.atTime(LocalTime.MAX);

        return detalleVentaRepository.productosMasVendidos(desdeDateTime, hastaDateTime).stream()
            .map(p -> new ProductoMasVendidoResponse(p.getProductoId(), p.getNombre(), p.getCantidadVendida(), p.getTotalVendido()))
            .toList();
    }

    /** RF-43: servicios realizados (domicilio y taller) en un rango de fechas. */
    @Transactional(readOnly = true)
    public ReporteServiciosResponse reporteServicios(LocalDate desde, LocalDate hasta) {
        var inicio = desde.atStartOfDay();
        var fin = hasta.atTime(LocalTime.MAX);
        var realizado = com.cerrajeria.sig.domicilio.EstadoServicio.REALIZADO;
        var cancelado = com.cerrajeria.sig.domicilio.EstadoServicio.CANCELADO;

        var domicilio = new ReporteServiciosResponse.Domicilio(
            servicioDomicilioRepository.countByEstadoAndFechaRealizadoBetween(realizado, inicio, fin),
            servicioDomicilioRepository.countByEstadoAndFechaBetween(cancelado, desde, hasta),
            servicioDomicilioRepository.cobradoEntre(inicio, fin),
            grupos(servicioDomicilioRepository.realizadosPorTipo(inicio, fin)),
            grupos(servicioDomicilioRepository.realizadosPorTecnico(inicio, fin)));

        var taller = new ReporteServiciosResponse.Taller(
            ordenTrabajoRepository.countByFechaIngresoBetween(inicio, fin),
            ordenTrabajoRepository.countByEstadoAndFechaEntregaBetween(
                com.cerrajeria.sig.ordenes.EstadoOrden.ENTREGADO, inicio, fin),
            ordenTrabajoRepository.countByEstadoNotIn(com.cerrajeria.sig.ordenes.EstadoOrden.FINALES),
            pagoOrdenRepository.ingresosNetosEntre(inicio, fin));

        return new ReporteServiciosResponse(domicilio, taller);
    }

    /**
     * RF-44: clientes que mas dejaron en un rango, sumando ventas, taller y
     * domicilio. {@code limite} recorta el ranking (los demas siguen contando
     * en {@code clientesConMovimiento}).
     */
    @Transactional(readOnly = true)
    public ReporteClientesResponse reporteClientes(LocalDate desde, LocalDate hasta, int limite) {
        var inicio = desde.atStartOfDay();
        var fin = hasta.atTime(LocalTime.MAX);

        var ventas = porClave(ventaRepository.comprasPorCliente(inicio, fin));
        var taller = porClave(pagoOrdenRepository.pagadoPorCliente(inicio, fin));
        var domicilio = porClave(servicioDomicilioRepository.cobradoPorCliente(inicio, fin));

        var nombres = new HashMap<Long, String>();
        Stream.of(ventas, taller, domicilio)
            .flatMap(m -> m.values().stream())
            .forEach(f -> nombres.putIfAbsent(f.getClave(), f.getNombre()));

        var ranking = nombres.entrySet().stream()
            .map(e -> {
                var v = movimiento(ventas.get(e.getKey()));
                var t = movimiento(taller.get(e.getKey()));
                var d = movimiento(domicilio.get(e.getKey()));
                return new ReporteClientesResponse.Cliente(e.getKey(), e.getValue(), v, t, d,
                    v.total().add(t.total()).add(d.total()));
            })
            .sorted(Comparator.comparing(ReporteClientesResponse.Cliente::total).reversed()
                .thenComparing(ReporteClientesResponse.Cliente::nombre))
            .toList();

        return new ReporteClientesResponse(ranking.size(),
            ventaRepository.totalSinClienteEntreFechas(inicio, fin),
            ranking.stream().limit(Math.max(1, limite)).toList());
    }

    private static List<ReporteServiciosResponse.Grupo> grupos(List<TotalAgrupadoProjection> filas) {
        return filas.stream()
            .map(f -> new ReporteServiciosResponse.Grupo(f.getNombre(), f.getCantidad(), f.getTotal()))
            .toList();
    }

    private static Map<Long, TotalAgrupadoProjection> porClave(List<TotalAgrupadoProjection> filas) {
        var mapa = new HashMap<Long, TotalAgrupadoProjection>();
        filas.forEach(f -> mapa.put(f.getClave(), f));
        return mapa;
    }

    private static ReporteClientesResponse.Movimiento movimiento(TotalAgrupadoProjection fila) {
        return fila == null ? ReporteClientesResponse.Movimiento.NINGUNO
            : new ReporteClientesResponse.Movimiento(fila.getCantidad(), fila.getTotal());
    }

    /** RF-41: reporte general de inventario con valorización. */
    @Transactional(readOnly = true)
    public ReporteInventarioResponse reporteInventario() {
        var productos = productoRepository.findAll().stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).toList();

        var items = productos.stream()
            .map(p -> new ReporteInventarioResponse.ItemInventario(
                p.getCodigo(), p.getNombre(), p.getTipoProducto().name(),
                p.getCantidadStock(), p.getPrecioVenta(),
                p.getPrecioVenta().multiply(BigDecimal.valueOf(p.getCantidadStock())),
                p.tieneBajoStock()
            ))
            .toList();

        var totalUnidades = productos.stream().mapToInt(p -> p.getCantidadStock()).sum();
        var valorTotal = items.stream().map(ReporteInventarioResponse.ItemInventario::valorLinea)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReporteInventarioResponse(productos.size(), totalUnidades, valorTotal, items);
    }
}
