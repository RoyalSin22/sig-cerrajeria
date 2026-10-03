package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.inventario.dto.AjusteStockRequest;
import com.cerrajeria.sig.inventario.dto.MovimientoInventarioResponse;
import com.cerrajeria.sig.inventario.dto.ProductoCreateRequest;
import com.cerrajeria.sig.inventario.dto.ProductoResponse;
import com.cerrajeria.sig.inventario.dto.ProductoUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
@Tag(name = "Productos / Inventario")
public class ProductoController {

    private final ProductoService productoService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar un nuevo producto en el catálogo (RF-11)")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @GetMapping
    @Operation(summary = "Listado paginado del catálogo completo, opcionalmente filtrado por tipo")
    public ResponseEntity<org.springframework.data.domain.Page<ProductoResponse>> listar(
            @RequestParam(required = false) TipoProducto tipoProducto,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(productoService.listar(tipoProducto, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar la ficha de un producto")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtener(id));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscador instantáneo por nombre o código (RF-46)")
    public ResponseEntity<List<ProductoResponse>> buscar(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            throw new IllegalArgumentException("El término de búsqueda debe tener al menos 2 caracteres");
        }
        return ResponseEntity.ok(productoService.buscar(q.trim()));
    }

    @GetMapping("/stock-bajo")
    @Operation(summary = "Productos en o por debajo de su stock mínimo (RF-13)")
    public ResponseEntity<List<ProductoResponse>> stockBajo() {
        return ResponseEntity.ok(productoService.listarStockBajo());
    }

    @PostMapping(value = "/{id}/imagen", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Subir o reemplazar la fotografía de un producto (RF-16)")
    public ResponseEntity<ProductoResponse> subirImagen(
            @PathVariable Long id,
            @RequestParam("archivo") org.springframework.web.multipart.MultipartFile archivo) {
        return ResponseEntity.ok(productoService.actualizarImagen(id, archivo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar los datos de un producto (no afecta el stock)")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ProductoUpdateRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dar de baja lógica a un producto")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/kardex")
    @Operation(summary = "Historial de movimientos de inventario del producto (RF-14)")
    public ResponseEntity<List<MovimientoInventarioResponse>> historial(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.historial(id));
    }

    @PostMapping("/{id}/ajustar-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ajuste manual de stock con motivo obligatorio (RF-15)")
    public ResponseEntity<ProductoResponse> ajustarStock(
            @PathVariable Long id, @Valid @RequestBody AjusteStockRequest request) {
        return ResponseEntity.ok(productoService.ajustarStock(id, request));
    }
}
