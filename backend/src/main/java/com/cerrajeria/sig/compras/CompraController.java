package com.cerrajeria.sig.compras;

import com.cerrajeria.sig.compras.dto.CompraCreateRequest;
import com.cerrajeria.sig.compras.dto.CompraLoteRequest;
import com.cerrajeria.sig.compras.dto.CompraResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
@Tag(name = "Compras")
public class CompraController {

    private final CompraService compraService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar una compra a proveedor (RF-27 a RF-29)")
    public ResponseEntity<CompraResponse> crear(@Valid @RequestBody CompraCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.crear(request));
    }

    @PostMapping("/lote")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar varios productos comprados al mismo proveedor, todos o ninguno")
    public ResponseEntity<List<CompraResponse>> crearLote(@Valid @RequestBody CompraLoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.crearLote(request));
    }

    @GetMapping
    @Operation(summary = "Listado paginado de compras")
    public ResponseEntity<Page<CompraResponse>> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(compraService.listar(pageable));
    }
}
