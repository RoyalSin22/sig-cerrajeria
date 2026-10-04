package com.cerrajeria.sig.compras;

import com.cerrajeria.sig.common.MetodoPago;
import com.cerrajeria.sig.caja.CajaDiaria;
import com.cerrajeria.sig.caja.CajaService;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.compras.dto.CompraCreateRequest;
import com.cerrajeria.sig.compras.dto.CompraResponse;
import com.cerrajeria.sig.inventario.Producto;
import com.cerrajeria.sig.inventario.ProductoService;
import com.cerrajeria.sig.inventario.ReferenciaTipo;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** RF-27 a RF-29: toda compra actualiza el inventario y genera Kardex (regla de negocio 2). */
@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final UsuarioRepository usuarioRepository;
    private final CajaService cajaService;

    @Transactional
    public CompraResponse crear(CompraCreateRequest request) {
        var proveedor = proveedorService.buscarPorId(request.proveedorId());

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setCantidad(request.cantidad());
        compra.setPrecioUnitario(request.precioUnitario());
        compra.setUsuario(usuarioActual());

        BigDecimal total = request.precioUnitario().multiply(BigDecimal.valueOf(request.cantidad()));

        if (request.pagarDeCaja()) {
            // Regla de negocio 1: si la compra sale de caja, exige caja abierta.
            CajaDiaria caja = cajaService.obtenerCajaAbiertaEntidad();
            compra.setCajaDiaria(caja);
            cajaService.sumarEgresoACajaActiva(caja, total, MetodoPago.EFECTIVO);
        }

        compra = compraRepository.save(compra);

        // Regla de negocio 2: toda entrada de stock genera Kardex.
        Producto producto = productoService.incrementarStock(
            request.productoId(), request.cantidad(), ReferenciaTipo.COMPRA, compra.getId());
        compra.setProducto(producto);
        compra = compraRepository.save(compra);

        return toResponse(compra, proveedor.getNombre(), producto.getNombre(), total);
    }

    /** Listado paginado de compras. */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CompraResponse> listar(org.springframework.data.domain.Pageable pageable) {
        return compraRepository.findAllByOrderByFechaDesc(pageable)
            .map(c -> toResponse(c, c.getProveedor().getNombre(), c.getProducto().getNombre(),
                c.getPrecioUnitario().multiply(BigDecimal.valueOf(c.getCantidad()))));
    }

    private CompraResponse toResponse(Compra compra, String proveedorNombre, String productoNombre, BigDecimal total) {
        return new CompraResponse(
            compra.getId(), proveedorNombre, productoNombre,
            compra.getCantidad(), compra.getPrecioUnitario(), total, compra.getFecha());
    }

    private Usuario usuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
