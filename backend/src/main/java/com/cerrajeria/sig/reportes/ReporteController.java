package com.cerrajeria.sig.reportes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes")
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/ventas")
    @Operation(summary = "Reporte de ventas en un rango de fechas (RF-38)")
    public ResponseEntity<ReporteVentasResponse> ventas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.reporteVentas(desde, hasta));
    }

    @GetMapping("/productos-mas-vendidos")
    @Operation(summary = "Productos más vendidos en un rango de fechas (RF-39)")
    public ResponseEntity<List<ProductoMasVendidoResponse>> productosMasVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.productosMasVendidos(desde, hasta));
    }

    @GetMapping("/inventario")
    @Operation(summary = "Reporte general de inventario con valorización (RF-41)")
    public ResponseEntity<ReporteInventarioResponse> inventario() {
        return ResponseEntity.ok(reporteService.reporteInventario());
    }
}
