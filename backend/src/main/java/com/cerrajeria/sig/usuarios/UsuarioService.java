package com.cerrajeria.sig.usuarios;

import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.usuarios.dto.CambiarEstadoUsuarioRequest;
import com.cerrajeria.sig.usuarios.dto.UsuarioCreateRequest;
import com.cerrajeria.sig.usuarios.dto.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestion de usuarios (ADMIN/EMPLOYEE). Solo un ADMIN puede crear o desactivar usuarios. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        usuarioRepository.findByUsername(request.username()).ifPresent(u -> {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre: " + request.username());
        });

        Role role;
        try {
            role = Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Rol inválido: use ROLE_ADMIN o ROLE_EMPLOYEE");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setNombre(request.nombre());
        usuario.setRole(role);
        usuario.setActivo(true);

        return toResponse(usuarioRepository.save(usuario));
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, CambiarEstadoUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
        usuario.setActivo(request.activo());
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse vincularWhatsapp(Long id, com.cerrajeria.sig.usuarios.dto.VincularWhatsappRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        usuarioRepository.findByTelefonoWhatsappAndActivoTrue(request.telefonoWhatsapp()).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new IllegalArgumentException("Ese teléfono ya está vinculado a otro usuario");
            }
        });

        usuario.setTelefonoWhatsapp(request.telefonoWhatsapp());
        return toResponse(usuarioRepository.save(usuario));
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getNombre(), u.getRole().name(), u.getActivo(), u.getTelefonoWhatsapp());
    }
}
