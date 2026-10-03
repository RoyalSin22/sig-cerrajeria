package com.cerrajeria.sig.ventas;

import com.cerrajeria.sig.ventas.dto.VentaCreateRequest;
import com.cerrajeria.sig.ventas.dto.VentaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas")
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    @Operation(summary = "Registrar una venta de mostrador (RF-30 a RF-33)")
    public ResponseEntity<VentaResponse> crear(@Valid @RequestBody VentaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crear(request));
    }

    @GetMapping
    @Operation(summary = "Listado paginado de ventas")
    public ResponseEntity<Page<VentaResponse>> listar(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ventaService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtener(id));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Anular una venta: revierte stock (Kardex) y el ingreso de caja si sigue abierta")
    public ResponseEntity<VentaResponse> anular(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.anular(id));
    }
}
