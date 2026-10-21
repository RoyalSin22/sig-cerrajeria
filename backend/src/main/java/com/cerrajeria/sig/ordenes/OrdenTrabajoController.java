package com.cerrajeria.sig.ordenes;

import com.cerrajeria.sig.ordenes.dto.CambiarEstadoOrdenRequest;
import com.cerrajeria.sig.ordenes.dto.OrdenTrabajoCreateRequest;
import com.cerrajeria.sig.ordenes.dto.OrdenTrabajoResponse;
import com.cerrajeria.sig.ordenes.dto.RegistrarPagoOrdenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ordenes")
@RequiredArgsConstructor
@Tag(name = "Órdenes de trabajo")
public class OrdenTrabajoController {

    private final OrdenTrabajoService ordenTrabajoService;

    @PostMapping
    @Operation(summary = "Crear una orden de trabajo, con adelanto opcional (RF-20, RF-21, RF-24)")
    public ResponseEntity<OrdenTrabajoResponse> crear(@Valid @RequestBody OrdenTrabajoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenTrabajoService.crear(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenTrabajoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenTrabajoService.obtener(id));
    }

    @GetMapping
    @Operation(summary = "Listar órdenes, opcionalmente filtradas por estado")
    public ResponseEntity<List<OrdenTrabajoResponse>> listar(@RequestParam(required = false) String estado) {
        return ResponseEntity.ok(ordenTrabajoService.listar(estado));
    }

    @GetMapping("/paginado")
    @Operation(summary = "Listado paginado de órdenes, opcionalmente filtrado por estado")
    public ResponseEntity<org.springframework.data.domain.Page<OrdenTrabajoResponse>> listarPaginado(
            @RequestParam(required = false) String estado,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(ordenTrabajoService.listarPaginado(estado, pageable));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de la orden (RF-22); bloquea ENTREGADO con saldo pendiente")
    public ResponseEntity<OrdenTrabajoResponse> cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody CambiarEstadoOrdenRequest request) {
        return ResponseEntity.ok(ordenTrabajoService.cambiarEstado(id, request));
    }

    @PostMapping("/{id}/pagos")
    @Operation(summary = "Registrar un adelanto o abono parcial contra la orden")
    public ResponseEntity<OrdenTrabajoResponse> registrarPago(
            @PathVariable Long id, @Valid @RequestBody RegistrarPagoOrdenRequest request) {
        return ResponseEntity.ok(ordenTrabajoService.registrarAdelanto(id, request));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar la orden, devolviendo o no el adelanto (egreso de la caja de quien lo devuelve)")
    public ResponseEntity<OrdenTrabajoResponse> cancelar(
            @PathVariable Long id, @Valid @RequestBody com.cerrajeria.sig.ordenes.dto.CancelarOrdenRequest request) {
        return ResponseEntity.ok(ordenTrabajoService.cancelar(id, request));
    }

    @PostMapping("/{id}/garantia")
    @Operation(summary = "Abrir una garantía (orden sin cobro) sobre una orden entregada")
    public ResponseEntity<OrdenTrabajoResponse> garantia(
            @PathVariable Long id, @Valid @RequestBody com.cerrajeria.sig.ordenes.dto.GarantiaRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
            .body(ordenTrabajoService.abrirGarantia(id, request));
    }

    @GetMapping("/{id}/materiales")
    @Operation(summary = "Material del inventario usado en la orden")
    public ResponseEntity<List<com.cerrajeria.sig.ordenes.dto.MaterialOrdenResponse>> materiales(@PathVariable Long id) {
        return ResponseEntity.ok(ordenTrabajoService.materiales(id));
    }

    @PostMapping("/{id}/materiales")
    @Operation(summary = "Registrar material usado en la orden: sale del stock por el Kárdex")
    public ResponseEntity<List<com.cerrajeria.sig.ordenes.dto.MaterialOrdenResponse>> usarMaterial(
            @PathVariable Long id, @Valid @RequestBody com.cerrajeria.sig.ordenes.dto.MaterialOrdenRequest request) {
        return ResponseEntity.ok(ordenTrabajoService.usarMaterial(id, request));
    }

    @PostMapping("/{id}/entregar")
    @Operation(summary = "Entregar la orden; si hay saldo pendiente, exige liquidarlo en la misma llamada (regla de negocio 3)")
    public ResponseEntity<OrdenTrabajoResponse> entregar(
            @PathVariable Long id, @RequestBody(required = false) RegistrarPagoOrdenRequest pagoOpcional) {
        return ResponseEntity.ok(ordenTrabajoService.entregarConLiquidacion(id, pagoOpcional));
    }
}
