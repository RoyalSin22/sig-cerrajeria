package com.cerrajeria.sig.ventas;

import com.cerrajeria.sig.caja.CajaDiaria;
import com.cerrajeria.sig.caja.CajaService;
import com.cerrajeria.sig.clientes.Cliente;
import com.cerrajeria.sig.clientes.ClienteRepository;
import com.cerrajeria.sig.common.MetodoPago;
import com.cerrajeria.sig.common.audit.AuditAccion;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.inventario.Producto;
import com.cerrajeria.sig.inventario.ProductoService;
import com.cerrajeria.sig.inventario.ReferenciaTipo;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import com.cerrajeria.sig.ventas.dto.DesglosePagoRequest;
import com.cerrajeria.sig.ventas.dto.DesglosePagoResponse;
import com.cerrajeria.sig.ventas.dto.DetalleVentaResponse;
import com.cerrajeria.sig.ventas.dto.ItemVentaRequest;
import com.cerrajeria.sig.ventas.dto.VentaCreateRequest;
import com.cerrajeria.sig.ventas.dto.VentaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaService cajaService;
    private final ProductoService productoService;
    private final AuditLogService auditLogService;

    /**
     * Registra una venta de mostrador (RF-30 a RF-33). Bloquea si no hay caja
     * abierta (regla 1) y descuenta stock generando Kardex por cada item
     * (regla 2), todo en una unica transaccion. Si el metodo de pago es
     * MIXTO, exige un desglose que sume exactamente el total.
     */
    @Transactional
    public VentaResponse crear(VentaCreateRequest request) {
        CajaDiaria caja = cajaService.obtenerCajaAbiertaEntidad();

        MetodoPago metodoPago = parseMetodoPago(request.metodoPago());

        Venta venta = new Venta();
        venta.setCajaDiaria(caja);
        venta.setUsuario(usuarioActual());
        venta.setMetodoPago(metodoPago);
        if (request.clienteId() != null) {
            venta.setCliente(buscarCliente(request.clienteId()));
        }

        BigDecimal total = BigDecimal.ZERO;
        venta.setMontoTotal(BigDecimal.ZERO);
        venta = ventaRepository.save(venta); // para obtener el id, usado como referenciaId del Kardex

        for (ItemVentaRequest item : request.items()) {
            // Descuenta stock y genera el movimiento de Kardex (regla de negocio 2)
            Producto producto = productoService.descontarStock(
                item.productoId(), item.cantidad(), ReferenciaTipo.VENTA, venta.getId());

            BigDecimal precio = precioDeLinea(producto, item.esDuplicado());
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(item.cantidad()));
            total = total.add(subtotal);

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(precio);
            detalle.setDuplicado(item.esDuplicado());
            venta.getDetalles().add(detalle);
        }

        venta.setMontoTotal(total);
        venta = ventaRepository.save(venta);

        if (metodoPago == MetodoPago.MIXTO) {
            guardarDesglose(venta, request.desglosePago(), total);
        }

        // Toda venta es un ingreso de caja (regla de negocio 1: queda ligada a la caja activa),
        // registrado por metodo para que Yape/Plin/tarjeta no se cuenten como efectivo.
        if (metodoPago == MetodoPago.MIXTO) {
            for (DesglosePagoRequest linea : request.desglosePago()) {
                cajaService.sumarIngresoACajaActiva(caja, linea.monto(), parseMetodoPagoSimple(linea.metodoPago()));
            }
        } else {
            cajaService.sumarIngresoACajaActiva(caja, total, metodoPago);
        }

        return mapearAResponse(venta);
    }

    private void guardarDesglose(Venta venta, List<DesglosePagoRequest> desglose, BigDecimal total) {
        if (desglose == null || desglose.isEmpty()) {
            throw new IllegalArgumentException(
                "El pago mixto requiere el desglose por método (ej. efectivo + Yape)");
        }

        BigDecimal suma = BigDecimal.ZERO;
        for (DesglosePagoRequest linea : desglose) {
            MetodoPago metodo = parseMetodoPagoSimple(linea.metodoPago());
            PagoVenta pago = new PagoVenta();
            pago.setVenta(venta);
            pago.setMetodoPago(metodo);
            pago.setMonto(linea.monto());
            pagoVentaRepository.save(pago);
            suma = suma.add(linea.monto());
        }

        if (suma.compareTo(total) != 0) {
            throw new IllegalArgumentException(
                "El desglose del pago mixto (" + suma + ") no coincide con el total de la venta (" + total + ")");
        }
    }

    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id) {
        return mapearAResponse(buscarPorId(id));
    }

    /** Listado paginado de ventas (incluye anuladas, marcadas con el flag correspondiente). */
    @Transactional(readOnly = true)
    public Page<VentaResponse> listar(Pageable pageable) {
        return ventaRepository.findAllByOrderByFechaHoraDesc(pageable).map(this::mapearAResponse);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> historialPorCliente(Long clienteId) {
        return ventaRepository.findByClienteIdOrderByFechaHoraDesc(clienteId).stream()
            .map(this::mapearAResponse)
            .toList();
    }

    /**
     * Anula una venta: revierte el stock de cada item (Kardex de entrada) y,
     * si la caja que la proceso sigue abierta, revierte tambien el ingreso.
     * Si la caja ya se cerro, se rechaza (no se puede alterar una caja
     * cerrada sin romper la regla de negocio 1) y debe resolverse como un
     * ajuste manual documentado.
     */
    @Transactional
    public VentaResponse anular(Long id) {
        Venta venta = buscarPorId(id);

        if (Boolean.TRUE.equals(venta.getAnulada())) {
            throw new IllegalArgumentException("La venta ya se encuentra anulada");
        }
        if (venta.getCajaDiaria().getEstado() != com.cerrajeria.sig.caja.EstadoCaja.ABIERTA) {
            throw new IllegalArgumentException(
                "No se puede anular: la caja que procesó esta venta ya está cerrada");
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            productoService.incrementarStock(
                detalle.getProducto().getId(), detalle.getCantidad(), ReferenciaTipo.VENTA, venta.getId());
        }

        // Se devuelve por el mismo metodo con que se cobro.
        if (venta.getMetodoPago() == MetodoPago.MIXTO) {
            for (PagoVenta pago : pagoVentaRepository.findByVentaId(venta.getId())) {
                cajaService.sumarEgresoACajaActiva(venta.getCajaDiaria(), pago.getMonto(), pago.getMetodoPago());
            }
        } else {
            cajaService.sumarEgresoACajaActiva(venta.getCajaDiaria(), venta.getMontoTotal(), venta.getMetodoPago());
        }

        venta.setAnulada(true);
        venta.setFechaAnulacion(LocalDateTime.now());
        venta = ventaRepository.save(venta);

        auditLogService.registrar(usuarioActual(), AuditAccion.ANULACION, "Venta", venta.getId());

        return mapearAResponse(venta);
    }

    /** Un duplicado cobra material + corte; la llave sola, su precio de venta. */
    private static BigDecimal precioDeLinea(Producto producto, boolean duplicado) {
        if (!duplicado) {
            return producto.getPrecioVenta();
        }
        if (producto.getPrecioDuplicado() == null) {
            throw new IllegalArgumentException(producto.getNombre() + " no tiene precio de duplicado");
        }
        return producto.getPrecioDuplicado();
    }

    private VentaResponse mapearAResponse(Venta venta) {
        List<DetalleVentaResponse> detalles = venta.getDetalles().stream()
            .map(d -> new DetalleVentaResponse(
                d.getProducto().getId(),
                d.getProducto().getNombre(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())),
                d.isDuplicado()
            ))
            .toList();

        List<DesglosePagoResponse> desglose = venta.getMetodoPago() == MetodoPago.MIXTO
            ? pagoVentaRepository.findByVentaId(venta.getId()).stream()
                .map(p -> new DesglosePagoResponse(p.getMetodoPago().name(), p.getMonto()))
                .toList()
            : List.of();

        return new VentaResponse(
            venta.getId(),
            venta.getCliente() != null ? venta.getCliente().getNombre() : null,
            venta.getUsuario().getNombre(),
            venta.getFechaHora(),
            venta.getMontoTotal(),
            venta.getMetodoPago().name(),
            detalles,
            venta.getAnulada(),
            desglose
        );
    }

    private Venta buscarPorId(Long id) {
        return ventaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada: " + id));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
    }

    private MetodoPago parseMetodoPago(String valor) {
        try {
            return MetodoPago.valueOf(valor.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Método de pago inválido");
        }
    }

    /** Para lineas de desglose: no tiene sentido que una linea individual sea MIXTO. */
    private MetodoPago parseMetodoPagoSimple(String valor) {
        MetodoPago metodo = parseMetodoPago(valor);
        if (metodo == MetodoPago.MIXTO) {
            throw new IllegalArgumentException("Una línea del desglose no puede ser MIXTO");
        }
        return metodo;
    }

    private Usuario usuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
