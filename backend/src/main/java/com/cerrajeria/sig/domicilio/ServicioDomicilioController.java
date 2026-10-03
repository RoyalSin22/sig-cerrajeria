package com.cerrajeria.sig.domicilio;

import com.cerrajeria.sig.domicilio.dto.CambiarEstadoServicioRequest;
import com.cerrajeria.sig.domicilio.dto.ServicioDomicilioRequest;
import com.cerrajeria.sig.domicilio.dto.ServicioDomicilioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/servicios-domicilio")
@RequiredArgsConstructor
@Tag(name = "Servicios a domicilio")
public class ServicioDomicilioController {

    private final ServicioDomicilioService servicioDomicilioService;

    @PostMapping
    @Operation(summary = "Agendar un servicio a domicilio (RF-25)")
    public ResponseEntity<ServicioDomicilioResponse> crear(@Valid @RequestBody ServicioDomicilioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioDomicilioService.crear(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioDomicilioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(servicioDomicilioService.obtener(id));
    }

    @GetMapping("/agenda")
    @Operation(summary = "Agenda semanal o mensual según el rango de fechas dado (RF-26)")
    public ResponseEntity<List<ServicioDomicilioResponse>> agenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(servicioDomicilioService.agenda(desde, hasta));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ServicioDomicilioResponse> cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody CambiarEstadoServicioRequest request) {
        return ResponseEntity.ok(servicioDomicilioService.cambiarEstado(id, request));
    }
}
