package com.cerrajeria.sig.asistente;

import com.cerrajeria.sig.asistente.dto.AsistenteMensajeRequest;
import com.cerrajeria.sig.asistente.dto.AsistenteMensajeResponse;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Chat con el asistente de IA dentro de la app (Fase 12). A diferencia del
 * canal de WhatsApp, aqui el usuario ya viene autenticado por el JWT normal
 * — no hace falta resolverlo por telefono.
 */
@RestController
@RequestMapping("/api/v1/asistente")
@RequiredArgsConstructor
@Tag(name = "Asistente IA")
public class AsistenteController {

    private final AsistenteIAService asistenteIAService;
    private final UsuarioRepository usuarioRepository;

    @org.springframework.beans.factory.annotation.Value("${sig.anthropic.api-key:}")
    private String apiKey;

    /**
     * Si el asistente esta configurado (hay clave de Anthropic). Sin clave, la
     * app no muestra el boton del chat: un boton que solo responde con errores
     * confunde mas de lo que ayuda.
     */
    @org.springframework.web.bind.annotation.GetMapping("/estado")
    @Operation(summary = "Indica si el asistente de IA está disponible (hay clave configurada)")
    public ResponseEntity<java.util.Map<String, Boolean>> estado() {
        return ResponseEntity.ok(java.util.Map.of("disponible", apiKey != null && !apiKey.isBlank()));
    }

    @PostMapping("/mensaje")
    @Operation(summary = "Enviar un mensaje al asistente de IA y recibir su respuesta")
    public ResponseEntity<AsistenteMensajeResponse> enviarMensaje(@Valid @RequestBody AsistenteMensajeRequest request) {
        Usuario usuario = usuarioActual();
        List<com.cerrajeria.sig.asistente.dto.TurnoChat> historial =
            request.historial() != null ? request.historial() : List.of();

        String respuesta = asistenteIAService.responder(usuario, historial, request.mensaje());
        return ResponseEntity.ok(new AsistenteMensajeResponse(respuesta));
    }

    private Usuario usuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
