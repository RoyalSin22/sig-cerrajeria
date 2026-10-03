package com.cerrajeria.sig.usuarios;

import com.cerrajeria.sig.usuarios.dto.CambiarEstadoUsuarioRequest;
import com.cerrajeria.sig.usuarios.dto.UsuarioCreateRequest;
import com.cerrajeria.sig.usuarios.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Solo ADMIN gestiona usuarios: es quien da de alta al empleado de mostrador. */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @Operation(summary = "Registrar un nuevo usuario (administrador o empleado)")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activar o desactivar un usuario")
    public ResponseEntity<UsuarioResponse> cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, request));
    }

    @PatchMapping("/{id}/whatsapp")
    @Operation(summary = "Vincular el teléfono de WhatsApp desde el que ese usuario podrá operar el sistema")
    public ResponseEntity<UsuarioResponse> vincularWhatsapp(
            @PathVariable Long id, @Valid @RequestBody com.cerrajeria.sig.usuarios.dto.VincularWhatsappRequest request) {
        return ResponseEntity.ok(usuarioService.vincularWhatsapp(id, request));
    }
}
