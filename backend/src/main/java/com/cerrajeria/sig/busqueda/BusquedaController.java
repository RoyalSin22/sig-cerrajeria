package com.cerrajeria.sig.busqueda;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/buscar")
@RequiredArgsConstructor
@Tag(name = "Buscador global")
public class BusquedaController {

    private final BusquedaService busquedaService;

    @GetMapping
    @Operation(summary = "Buscador global instantáneo: productos, clientes y órdenes (RF-46)")
    public ResponseEntity<ResultadoBusquedaResponse> buscar(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            throw new IllegalArgumentException("El término de búsqueda debe tener al menos 2 caracteres");
        }
        return ResponseEntity.ok(busquedaService.buscar(q.trim()));
    }
}
