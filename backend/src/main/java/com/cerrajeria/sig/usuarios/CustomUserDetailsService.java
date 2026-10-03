package com.cerrajeria.sig.usuarios;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos"));

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new DisabledException("Usuario inactivo");
        }

        return User.builder()
            .username(usuario.getUsername())
            .password(usuario.getPassword())
            .authorities(usuario.getRole().name())
            .build();
    }
}
