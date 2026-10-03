package com.cerrajeria.sig.domicilio;

import com.cerrajeria.sig.clientes.Cliente;
import com.cerrajeria.sig.clientes.ClienteRepository;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.domicilio.dto.CambiarEstadoServicioRequest;
import com.cerrajeria.sig.domicilio.dto.ServicioDomicilioRequest;
import com.cerrajeria.sig.domicilio.dto.ServicioDomicilioResponse;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicioDomicilioService {

    private final ServicioDomicilioRepository servicioDomicilioRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioDomicilioMapper mapper;

    @Transactional
    public ServicioDomicilioResponse crear(ServicioDomicilioRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
            .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + request.clienteId()));

        ServicioDomicilio servicio = new ServicioDomicilio();
        servicio.setCliente(cliente);
        servicio.setDireccion(request.direccion());
        servicio.setTelefono(request.telefono());
        servicio.setFecha(request.fecha());
        servicio.setHora(request.hora());
        servicio.setTipoServicio(request.tipoServicio());
        servicio.setObservaciones(request.observaciones());
        servicio.setUsuario(usuarioActual());

        return mapper.toResponse(servicioDomicilioRepository.save(servicio));
    }

    @Transactional(readOnly = true)
    public ServicioDomicilioResponse obtener(Long id) {
        return mapper.toResponse(buscarPorId(id));
    }

    /** Agenda semanal/mensual (RF-26): se define el rango desde el cliente (semana o mes). */
    @Transactional(readOnly = true)
    public List<ServicioDomicilioResponse> agenda(LocalDate desde, LocalDate hasta) {
        return mapper.toResponseList(
            servicioDomicilioRepository.findByFechaBetweenOrderByFechaAscHoraAsc(desde, hasta));
    }

    @Transactional(readOnly = true)
    public List<ServicioDomicilioResponse> historialPorCliente(Long clienteId) {
        return mapper.toResponseList(servicioDomicilioRepository.findByClienteIdOrderByFechaDesc(clienteId));
    }

    @Transactional
    public ServicioDomicilioResponse cambiarEstado(Long id, CambiarEstadoServicioRequest request) {
        ServicioDomicilio servicio = buscarPorId(id);
        try {
            servicio.setEstado(EstadoServicio.valueOf(request.nuevoEstado().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado inválido: " + request.nuevoEstado());
        }
        return mapper.toResponse(servicioDomicilioRepository.save(servicio));
    }

    private ServicioDomicilio buscarPorId(Long id) {
        return servicioDomicilioRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Servicio a domicilio no encontrado: " + id));
    }

    private Usuario usuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
