package com.cerrajeria.sig.caja;

import com.cerrajeria.sig.caja.dto.AperturaCajaRequest;
import com.cerrajeria.sig.caja.dto.CajaDiariaResponse;
import com.cerrajeria.sig.caja.dto.CierreCajaRequest;
import com.cerrajeria.sig.caja.dto.CierreCajaResponse;
import com.cerrajeria.sig.caja.dto.MovimientoCajaRequest;
import com.cerrajeria.sig.caja.dto.MovimientoCajaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/v1/caja")
@RequiredArgsConstructor
@Tag(name = "Caja diaria")
public class CajaController {

    private final CajaService cajaService;

    @PostMapping("/abrir")
    @Operation(summary = "Abrir la caja diaria del usuario en sesión (regla de negocio 1)")
    public ResponseEntity<CajaDiariaResponse> abrir(@Valid @RequestBody AperturaCajaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.abrir(request));
    }

    @GetMapping("/abierta")
    @Operation(summary = "Consultar la caja abierta del usuario en sesión")
    public ResponseEntity<CajaDiariaResponse> obtenerAbierta() {
        return ResponseEntity.ok(cajaService.obtenerCajaAbierta());
    }

    @PostMapping("/movimientos")
    @Operation(summary = "Registrar un ingreso o egreso manual de caja")
    public ResponseEntity<MovimientoCajaResponse> registrarMovimiento(@Valid @RequestBody MovimientoCajaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cajaService.registrarMovimientoManual(request));
    }

    @PostMapping("/cerrar")
    @Operation(summary = "Cerrar la caja diaria con arqueo")
    public ResponseEntity<CierreCajaResponse> cerrar(@Valid @RequestBody CierreCajaRequest request) {
        return ResponseEntity.ok(cajaService.cerrar(request));
    }

    @GetMapping("/historial")
    @Operation(summary = "Historial paginado de sesiones de caja del usuario en sesión (RF-37)")
    public ResponseEntity<Page<CajaDiariaResponse>> historial(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(cajaService.historialPaginado(pageable));
    }

    @GetMapping("/{id}/movimientos")
    @Operation(summary = "Historial de movimientos manuales de una caja")
    public ResponseEntity<List<MovimientoCajaResponse>> historialMovimientos(@PathVariable Long id) {
        return ResponseEntity.ok(cajaService.historialMovimientos(id));
    }
}
