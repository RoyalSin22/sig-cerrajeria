package com.cerrajeria.sig.clientes;

import com.cerrajeria.sig.clientes.dto.ClienteRequest;
import com.cerrajeria.sig.clientes.dto.ClienteResponse;
import com.cerrajeria.sig.ordenes.OrdenTrabajoService;
import com.cerrajeria.sig.ordenes.dto.OrdenTrabajoResponse;
import com.cerrajeria.sig.ventas.VentaService;
import com.cerrajeria.sig.ventas.dto.VentaResponse;
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
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final VentaService ventaService;
    private final OrdenTrabajoService ordenTrabajoService;

    @PostMapping
    @Operation(summary = "Registrar un cliente (RF-17)")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    @GetMapping
    @Operation(summary = "Listado paginado de todos los clientes")
    public ResponseEntity<Page<ClienteResponse>> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(clienteService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtener(id));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Búsqueda rápida por nombre o teléfono (RF-18)")
    public ResponseEntity<List<ClienteResponse>> buscar(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            throw new IllegalArgumentException("El término de búsqueda debe tener al menos 2 caracteres");
        }
        return ResponseEntity.ok(clienteService.buscar(q.trim()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @GetMapping("/{id}/compras")
    @Operation(summary = "Historial de compras del cliente (RF-19)")
    public ResponseEntity<List<VentaResponse>> historialCompras(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.historialPorCliente(id));
    }

    @GetMapping("/{id}/ordenes")
    @Operation(summary = "Historial de órdenes de trabajo del cliente (RF-19)")
    public ResponseEntity<List<OrdenTrabajoResponse>> historialOrdenes(@PathVariable Long id) {
        return ResponseEntity.ok(ordenTrabajoService.historialPorCliente(id));
    }
}
