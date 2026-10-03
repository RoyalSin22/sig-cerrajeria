package com.cerrajeria.sig.reportes;

import com.cerrajeria.sig.inventario.ProductoRepository;
import com.cerrajeria.sig.ventas.DetalleVentaRepository;
import com.cerrajeria.sig.ventas.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;

    public ReporteVentasResponse reporteVentas(LocalDate desde, LocalDate hasta) {
        var desdeDateTime = desde.atStartOfDay();
        var hastaDateTime = hasta.atTime(LocalTime.MAX);

        var total = ventaRepository.totalEntreFechas(desdeDateTime, hastaDateTime);
        var cantidad = ventaRepository.contarEntreFechas(desdeDateTime, hastaDateTime);
        var porMetodo = ventaRepository.totalPorMetodoPago(desdeDateTime, hastaDateTime).stream()
            .map(p -> new ReporteVentasResponse.VentaPorMetodoPago(p.getMetodoPago().name(), p.getTotal()))
            .toList();

        return new ReporteVentasResponse(total, cantidad, porMetodo);
    }

    public java.util.List<ProductoMasVendidoResponse> productosMasVendidos(LocalDate desde, LocalDate hasta) {
        var desdeDateTime = desde.atStartOfDay();
        var hastaDateTime = hasta.atTime(LocalTime.MAX);

        return detalleVentaRepository.productosMasVendidos(desdeDateTime, hastaDateTime).stream()
            .map(p -> new ProductoMasVendidoResponse(p.getProductoId(), p.getNombre(), p.getCantidadVendida(), p.getTotalVendido()))
            .toList();
    }

    /** RF-41: reporte general de inventario con valorización. */
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
