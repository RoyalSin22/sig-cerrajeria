package com.cerrajeria.sig.whatsapp;

import com.cerrajeria.sig.asistente.AsistenteIAService;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Adaptador del canal WhatsApp: resuelve que usuario escribe (por su
 * telefono), delega la interpretacion y ejecucion al mismo cerebro que usa
 * el chat dentro de la app (AsistenteIAService), y manda la respuesta de
 * vuelta por WhatsApp. WhatsApp no mantiene historial de conversacion entre
 * mensajes (cada mensaje se interpreta solo) — el chat web si lo mantiene.
 */
@Service
@RequiredArgsConstructor
public class ComandoWhatsAppService {

    private final UsuarioRepository usuarioRepository;
    private final AsistenteIAService asistenteIAService;
    private final WhatsAppClient whatsAppClient;

    @Async
    public void procesarMensaje(String telefonoRemitente, String texto) {
        var usuarioOpt = usuarioRepository.findByTelefonoWhatsappAndActivoTrue(telefonoRemitente);

        if (usuarioOpt.isEmpty()) {
            whatsAppClient.enviarTexto(telefonoRemitente,
                "Este número no está vinculado a ningún usuario del sistema. " +
                "Pídele a un administrador que lo vincule desde Ajustes → Usuarios.");
            return;
        }

        Usuario usuario = usuarioOpt.get();
        String respuesta = asistenteIAService.responder(usuario, List.of(), texto);
        whatsAppClient.enviarTexto(telefonoRemitente, respuesta);
    }
}
