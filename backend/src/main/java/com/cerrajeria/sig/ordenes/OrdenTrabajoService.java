package com.cerrajeria.sig.ordenes;

import com.cerrajeria.sig.caja.CajaDiaria;
import com.cerrajeria.sig.caja.CajaService;
import com.cerrajeria.sig.clientes.Cliente;
import com.cerrajeria.sig.clientes.ClienteRepository;
import com.cerrajeria.sig.common.MetodoPago;
import com.cerrajeria.sig.common.audit.AuditAccion;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.common.exception.SaldoPendienteException;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import com.cerrajeria.sig.ordenes.dto.CambiarEstadoOrdenRequest;
import com.cerrajeria.sig.ordenes.dto.OrdenTrabajoCreateRequest;
import com.cerrajeria.sig.ordenes.dto.OrdenTrabajoResponse;
import com.cerrajeria.sig.ordenes.dto.RegistrarPagoOrdenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Logica de negocio de ordenes de trabajo (RF-20 a RF-24, HU-12 a HU-14).
 * Aplica simultaneamente las reglas de negocio 1 (caja abierta para
 * adelantos/liquidaciones) y 3 (bloqueo de ENTREGADO con saldo pendiente).
 */
@Service
@RequiredArgsConstructor
public class OrdenTrabajoService {

    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final PagoOrdenRepository pagoOrdenRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaService cajaService;
    private final AuditLogService auditLogService;

    @Transactional
    public OrdenTrabajoResponse crear(OrdenTrabajoCreateRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
            .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + request.clienteId()));

        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setCliente(cliente);
        orden.setNumeroOrden("OT-" + String.format("%06d", ordenTrabajoRepository.siguienteNumeroSecuencia()));
        orden.setProductoRecibido(request.productoRecibido());
        orden.setProblema(request.problema());
        orden.setDiagnostico(request.diagnostico());
        orden.setPrecioTotal(request.precioTotal());
        orden.setFechaPrometida(request.fechaPrometida());
        orden.setObservaciones(request.observaciones());
        orden.setUsuario(usuarioActual());
        orden = ordenTrabajoRepository.save(orden);

        boolean hayAdelanto = request.adelanto() != null && request.adelanto().compareTo(BigDecimal.ZERO) > 0;
        if (hayAdelanto) {
            // Regla de negocio 1: un adelanto es dinero, exige caja abierta.
            registrarPago(orden, request.adelanto(), TipoPagoOrden.ADELANTO,
                parseMetodoPago(request.metodoPagoAdelanto()));
        }

        return toResponse(orden);
    }

    public OrdenTrabajoResponse obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    public List<OrdenTrabajoResponse> listar(String estado) {
        List<OrdenTrabajo> ordenes = (estado == null || estado.isBlank())
            ? ordenTrabajoRepository.findAllByOrderByFechaIngresoDesc()
            : ordenTrabajoRepository.findByEstadoOrderByFechaIngresoDesc(parseEstado(estado));
        return ordenes.stream().map(this::toResponse).toList();
    }

    /**
     * Version paginada, para listados largos fuera del tablero Kanban (que
     * necesita todas las ordenes agrupadas por estado y no se pagina).
     */
    public org.springframework.data.domain.Page<OrdenTrabajoResponse> listarPaginado(
            String estado, org.springframework.data.domain.Pageable pageable) {
        var pagina = (estado == null || estado.isBlank())
            ? ordenTrabajoRepository.findAllByOrderByFechaIngresoDesc(pageable)
            : ordenTrabajoRepository.findByEstadoOrderByFechaIngresoDesc(parseEstado(estado), pageable);
        return pagina.map(this::toResponse);
    }

    public List<OrdenTrabajoResponse> historialPorCliente(Long clienteId) {
        return ordenTrabajoRepository.findByClienteIdOrderByFechaIngresoDesc(clienteId)
            .stream().map(this::toResponse).toList();
    }

    /**
     * Cambia el estado de la orden. Si el destino es ENTREGADO y hay saldo
     * pendiente, se rechaza (regla de negocio 3): debe usarse
     * {@link #entregarConLiquidacion} en su lugar.
     */
    @Transactional
    public OrdenTrabajoResponse cambiarEstado(Long id, CambiarEstadoOrdenRequest request) {
        OrdenTrabajo orden = buscarPorId(id);
        EstadoOrden nuevoEstado = parseEstado(request.nuevoEstado());

        if (nuevoEstado == EstadoOrden.ENTREGADO && orden.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0) {
            throw new SaldoPendienteException(
                "No se puede entregar: saldo pendiente de " + orden.getSaldoPendiente()
                    + ". Use el endpoint de entrega con liquidación.");
        }

        orden.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoOrden.ENTREGADO) {
            orden.setFechaEntrega(java.time.LocalDateTime.now());
        }
        orden = ordenTrabajoRepository.save(orden);
        auditLogService.registrar(usuarioActual(), AuditAccion.CAMBIO_ESTADO, "OrdenTrabajo", orden.getId());
        return toResponse(orden);
    }

    /** Registra un adelanto o abono parcial, sin cambiar el estado de la orden. */
    @Transactional
    public OrdenTrabajoResponse registrarAdelanto(Long id, RegistrarPagoOrdenRequest request) {
        OrdenTrabajo orden = buscarPorId(id);
        if (request.monto().compareTo(orden.getSaldoPendiente()) > 0) {
            throw new IllegalArgumentException("El monto excede el saldo pendiente (" + orden.getSaldoPendiente() + ")");
        }
        registrarPago(orden, request.monto(), TipoPagoOrden.ADELANTO, parseMetodoPago(request.metodoPago()));
        return toResponse(orden);
    }

    /**
     * Implementa literalmente la regla de negocio 3: si hay saldo pendiente,
     * exige liquidarlo en la MISMA transaccion antes de marcar ENTREGADO.
     */
    @Transactional
    public OrdenTrabajoResponse entregarConLiquidacion(Long id, RegistrarPagoOrdenRequest pagoOpcional) {
        OrdenTrabajo orden = buscarPorId(id);
        BigDecimal saldo = orden.getSaldoPendiente();

        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            if (pagoOpcional == null || pagoOpcional.monto().compareTo(saldo) < 0) {
                throw new SaldoPendienteException(
                    "Debe liquidar el saldo pendiente completo (" + saldo + ") para entregar la orden");
            }
            registrarPago(orden, pagoOpcional.monto(), TipoPagoOrden.LIQUIDACION, parseMetodoPago(pagoOpcional.metodoPago()));
        }

        orden.setEstado(EstadoOrden.ENTREGADO);
        orden.setFechaEntrega(java.time.LocalDateTime.now());
        orden = ordenTrabajoRepository.save(orden);
        auditLogService.registrar(usuarioActual(), AuditAccion.CAMBIO_ESTADO, "OrdenTrabajo", orden.getId());
        return toResponse(orden);
    }

    private void registrarPago(OrdenTrabajo orden, BigDecimal monto, TipoPagoOrden tipoPago, MetodoPago metodoPago) {
        // Regla de negocio 1: todo pago exige una caja diaria ABIERTA.
        CajaDiaria caja = cajaService.obtenerCajaAbiertaEntidad();

        PagoOrden pago = new PagoOrden();
        pago.setOrdenTrabajo(orden);
        pago.setCajaDiaria(caja);
        pago.setMonto(monto);
        pago.setTipoPago(tipoPago);
        pago.setMetodoPago(metodoPago);
        pago.setUsuario(usuarioActual());
        pagoOrdenRepository.save(pago);

        orden.aplicarPago(monto);
        ordenTrabajoRepository.save(orden);
        cajaService.sumarIngresoACajaActiva(caja, monto);
    }

    private OrdenTrabajoResponse toResponse(OrdenTrabajo orden) {
        return new OrdenTrabajoResponse(
            orden.getId(),
            orden.getNumeroOrden(),
            orden.getCliente().getNombre(),
            orden.getProductoRecibido(),
            orden.getProblema(),
            orden.getDiagnostico(),
            orden.getPrecioTotal(),
            orden.getMontoPagado(),
            orden.getSaldoPendiente(),
            orden.getEstado().name(),
            orden.getFechaIngreso(),
            orden.getFechaPrometida(),
            orden.getFechaEntrega(),
            orden.getObservaciones()
        );
    }

    private EstadoOrden parseEstado(String estado) {
        try {
            return EstadoOrden.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado inválido: " + estado);
        }
    }

    private MetodoPago parseMetodoPago(String metodoPago) {
        if (metodoPago == null || metodoPago.isBlank()) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        try {
            return MetodoPago.valueOf(metodoPago.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Método de pago inválido: " + metodoPago);
        }
    }

    private OrdenTrabajo buscarPorId(Long id) {
        return ordenTrabajoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Orden de trabajo no encontrada: " + id));
    }

    private Usuario usuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
