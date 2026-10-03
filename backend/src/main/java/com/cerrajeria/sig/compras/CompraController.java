package com.cerrajeria.sig.compras;

import com.cerrajeria.sig.compras.dto.CompraCreateRequest;
import com.cerrajeria.sig.compras.dto.CompraResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @Operation(summary = "Listado paginado de compras")
    public ResponseEntity<org.springframework.data.domain.Page<CompraResponse>> listar(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(compraService.listar(pageable));
    }
}
