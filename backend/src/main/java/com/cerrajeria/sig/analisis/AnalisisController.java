package com.cerrajeria.sig.analisis;

import com.cerrajeria.sig.analisis.dto.ResultadoVentasAnteriores;
import com.cerrajeria.sig.analisis.dto.ResumenVentasAnteriores;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

/** Datos para Power BI y el historial de ventas de antes del sistema. Todo es del administrador. */
@RestController
@RequestMapping("/api/v1/analisis")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Análisis (Power BI)")
public class AnalisisController {

    private static final MediaType XLSX =
        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportacionPowerBiService exportacionPowerBiService;
    private final VentasAnterioresService ventasAnterioresService;

    @GetMapping("/power-bi")
    @Operation(summary = "Excel con una tabla por tema (ventas, cobros, taller, domicilio, productos, kárdex, compras, cajas)")
    public ResponseEntity<byte[]> powerBi(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok()
            // Siempre el mismo nombre: en Power BI basta reemplazar el archivo y pulsar Actualizar
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cerrajeria-datos.xlsx\"")
            .contentType(XLSX)
            .body(exportacionPowerBiService.exportar(desde, hasta));
    }

    @GetMapping("/ventas-anteriores")
    @Operation(summary = "Cuántas ventas anteriores hay cargadas, por cuánto y de qué fechas")
    public ResponseEntity<ResumenVentasAnteriores> resumenVentasAnteriores() {
        return ResponseEntity.ok(ventasAnterioresService.resumen());
    }

    @PostMapping(value = "/ventas-anteriores", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cargar ventas de antes del sistema desde CSV (revisar con confirmar=false); no tocan caja ni stock")
    public ResponseEntity<ResultadoVentasAnteriores> cargarVentasAnteriores(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(defaultValue = "false") boolean confirmar) throws IOException {
        return ResponseEntity.ok(ventasAnterioresService.importar(archivo.getBytes(), confirmar));
    }

    @DeleteMapping("/ventas-anteriores")
    @Operation(summary = "Borrar todas las ventas anteriores cargadas, para volver a subir el archivo corregido")
    public ResponseEntity<Map<String, Long>> borrarVentasAnteriores() {
        return ResponseEntity.ok(Map.of("borradas", ventasAnterioresService.borrarTodas()));
    }
}
