package com.cerrajeria.sig.domicilio;

import com.cerrajeria.sig.clientes.Cliente;
import com.cerrajeria.sig.clientes.ClienteRepository;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.caja.CajaDiaria;
import com.cerrajeria.sig.caja.CajaService;
import com.cerrajeria.sig.common.MetodoPago;
import com.cerrajeria.sig.domicilio.dto.CambiarEstadoServicioRequest;
import com.cerrajeria.sig.domicilio.dto.RealizarServicioRequest;
import com.cerrajeria.sig.domicilio.dto.ServicioDomicilioRequest;
import com.cerrajeria.sig.domicilio.dto.ServicioDomicilioResponse;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicioDomicilioService {

    private final ServicioDomicilioRepository servicioDomicilioRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioDomicilioMapper mapper;
    private final CajaService cajaService;

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

    /**
     * Cambios de estado sin dinero (EN_CAMINO, CANCELADO, volver a PROGRAMADO).
     * REALIZADO no pasa por aqui: lleva cobro, ver {@link #realizar}.
     */
    @Transactional
    public ServicioDomicilioResponse cambiarEstado(Long id, CambiarEstadoServicioRequest request) {
        ServicioDomicilio servicio = buscarPorId(id);
        EstadoServicio nuevo;
        try {
            nuevo = EstadoServicio.valueOf(request.nuevoEstado().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado inválido: " + request.nuevoEstado());
        }
        if (nuevo == EstadoServicio.REALIZADO) {
            throw new IllegalArgumentException("Para marcar el servicio como realizado hay que indicar lo cobrado");
        }
        exigirNoRealizado(servicio);
        servicio.setEstado(nuevo);
        return mapper.toResponse(servicioDomicilioRepository.save(servicio));
    }

    /**
     * Marca el servicio como REALIZADO y registra el cobro en la caja abierta
     * de quien lo realizo (regla de negocio 1): el tecnico lo cobra en el
     * lugar, desde su celular, y entra en SU cuadre de caja. Un monto 0 (sin
     * cobro) no mueve dinero y no exige caja.
     */
    @Transactional
    public ServicioDomicilioResponse realizar(Long id, RealizarServicioRequest request) {
        ServicioDomicilio servicio = buscarPorId(id);
        exigirNoRealizado(servicio);
        if (servicio.getEstado() == EstadoServicio.CANCELADO) {
            throw new IllegalArgumentException("El servicio está cancelado: vuelve a programarlo antes de marcarlo como realizado");
        }

        BigDecimal monto = request.monto();
        if (monto.signum() > 0) {
            MetodoPago metodo = parseMetodoPago(request.metodoPago());
            CajaDiaria caja = cajaService.obtenerCajaAbiertaEntidad();
            cajaService.sumarIngresoACajaActiva(caja, monto, metodo);
            servicio.setMetodoPago(metodo);
            servicio.setCajaDiaria(caja);
        }
        servicio.setMontoCobrado(monto);
        servicio.setRealizadoPor(usuarioActual());
        servicio.setFechaRealizado(LocalDateTime.now());
        servicio.setEstado(EstadoServicio.REALIZADO);
        return mapper.toResponse(servicioDomicilioRepository.save(servicio));
    }

    /** Un servicio realizado ya cobro: cambiarlo descuadraria la caja de quien lo cobro. */
    private void exigirNoRealizado(ServicioDomicilio servicio) {
        if (servicio.getEstado() == EstadoServicio.REALIZADO) {
            throw new IllegalArgumentException("El servicio ya fue realizado y cobrado: no se puede cambiar");
        }
    }

    private MetodoPago parseMetodoPago(String metodoPago) {
        if (metodoPago == null || metodoPago.isBlank()) {
            throw new IllegalArgumentException("Indica cómo se cobró (efectivo, Yape, Plin o tarjeta)");
        }
        MetodoPago metodo;
        try {
            metodo = MetodoPago.valueOf(metodoPago.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Método de pago inválido: " + metodoPago);
        }
        if (metodo == MetodoPago.MIXTO) {
            throw new IllegalArgumentException("El cobro de un servicio no admite pago mixto: usa un solo método");
        }
        return metodo;
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
