package com.cerrajeria.sig.negocio;

import com.cerrajeria.sig.negocio.dto.NegocioDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/negocio")
@RequiredArgsConstructor
@Tag(name = "Negocio")
public class NegocioController {

    private final NegocioService negocioService;

    /** Publico: el nombre sale en la pantalla de ingreso (es lo mismo que la tarjeta de presentacion). */
    @GetMapping
    @Operation(summary = "Nombre, teléfono y dirección del negocio")
    public ResponseEntity<NegocioDto> obtener() {
        return ResponseEntity.ok(negocioService.obtener());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar los datos del negocio")
    public ResponseEntity<NegocioDto> guardar(@Valid @RequestBody NegocioDto datos) {
        return ResponseEntity.ok(negocioService.guardar(datos));
    }
}
