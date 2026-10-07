package com.cerrajeria.sig.usuarios;

import lombok.RequiredArgsConstructor;
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

        // Se marca como deshabilitado en vez de lanzar DisabledException aqui:
        // DaoAuthenticationProvider envuelve las excepciones de este metodo y el
        // login respondia 500. Asi Spring hace su chequeo estandar y lanza
        // DisabledException, que GlobalExceptionHandler responde como 401.
        return User.builder()
            .username(usuario.getUsername())
            .password(usuario.getPassword())
            .authorities(usuario.getRole().name())
            .disabled(Boolean.FALSE.equals(usuario.getActivo()))
            .build();
    }
}
