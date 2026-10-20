package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.common.audit.AuditAccion;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.common.exception.StockInsuficienteException;
import com.cerrajeria.sig.inventario.dto.AjusteStockRequest;
import com.cerrajeria.sig.inventario.dto.MovimientoInventarioResponse;
import com.cerrajeria.sig.inventario.dto.ProductoCreateRequest;
import com.cerrajeria.sig.inventario.dto.ProductoResponse;
import com.cerrajeria.sig.inventario.dto.ProductoUpdateRequest;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logica de negocio del modulo de inventario (RF-11 a RF-16, HU-06/07/08).
 * Regla de negocio 2 (Fase 1): todo cambio de stock genera un movimiento de
 * Kardex en la misma transaccion.
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private static final String MOTIVO_STOCK_INICIAL = "Stock inicial al crear el producto";

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoMapper productoMapper;
    private final MovimientoInventarioMapper movimientoInventarioMapper;
    private final AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Value("${sig.uploads-dir}")
    private String uploadsDir;

    @Transactional
    public ProductoResponse crear(ProductoCreateRequest request) {
        return crear(request, MOTIVO_STOCK_INICIAL);
    }

    /**
     * Alta con un motivo propio para el movimiento de stock inicial en el
     * Kardex (ej. la importacion desde archivo, para distinguirla del alta manual).
     */
    @Transactional
    public ProductoResponse crear(ProductoCreateRequest request, String motivoStockInicial) {
        productoRepository.findByCodigo(request.codigo()).ifPresent(p -> {
            throw new IllegalArgumentException("Ya existe un producto con el código: " + request.codigo());
        });

        Producto producto = new Producto();
        producto.setCodigo(request.codigo());
        aplicarCamposComunes(producto, request.nombre(), request.tipoProducto(), request.marca(),
            request.stockMinimo(), request.precioCompra(), request.precioVenta(), request.imagenUrl(),
            request.llavePerfil(), request.llaveColor(), request.llaveDisenoTematica(),
            request.productoModelo(), request.productoTamano(), request.productoMaterial());
        producto.setPrecioDuplicado(request.tipoProducto().esLlave() ? request.precioDuplicado() : null);
        producto.setCantidadStock(0);
        producto = productoRepository.save(producto);

        if (request.cantidadStock() != null && request.cantidadStock() > 0) {
            producto.setCantidadStock(request.cantidadStock());
            productoRepository.save(producto);
            registrarMovimiento(producto, TipoMovimiento.ENTRADA, request.cantidadStock(),
                0, request.cantidadStock(), ReferenciaTipo.AJUSTE, null, motivoStockInicial);
        }

        return productoMapper.toResponse(producto);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return productoMapper.toResponse(buscarPorId(id));
    }

    /** Listado paginado del catálogo completo (explorar sin buscar), opcionalmente filtrado por tipo. */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProductoResponse> listar(
            TipoProducto tipoProducto, org.springframework.data.domain.Pageable pageable) {
        var pagina = tipoProducto != null
            ? productoRepository.findByActivoTrueAndTipoProductoOrderByNombreAsc(tipoProducto, pageable)
            : productoRepository.findByActivoTrueOrderByNombreAsc(pageable);
        return pagina.map(productoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> buscar(String termino) {
        return productoMapper.toResponseList(productoRepository.buscar(termino));
    }

    /** Las llaves que se ofrecen en "Duplicado de llave" (tienen precio de duplicado). */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarDuplicables() {
        return productoMapper.toResponseList(productoRepository.findByActivoTrueAndPrecioDuplicadoIsNotNullOrderByNombreAsc());
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarStockBajo() {
        return productoMapper.toResponseList(productoRepository.findStockBajo());
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoUpdateRequest request) {
        Producto producto = buscarPorId(id);
        aplicarCamposComunes(producto, request.nombre(), request.tipoProducto(), request.marca(),
            request.stockMinimo(), request.precioCompra(), request.precioVenta(), request.imagenUrl(),
            request.llavePerfil(), request.llaveColor(), request.llaveDisenoTematica(),
            request.productoModelo(), request.productoTamano(), request.productoMaterial());
        producto.setPrecioDuplicado(request.tipoProducto().esLlave() ? request.precioDuplicado() : null);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscarPorId(id);
        producto.setActivo(false);
        productoRepository.save(producto);
        auditLogService.registrar(usuarioActual(), com.cerrajeria.sig.common.audit.AuditAccion.ELIMINAR,
            "Producto", producto.getId());
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> historial(Long id) {
        buscarPorId(id);
        return movimientoInventarioMapper.toResponseList(
            movimientoInventarioRepository.findByProductoIdOrderByFechaHoraDesc(id));
    }

    @Transactional
    public ProductoResponse ajustarStock(Long id, AjusteStockRequest request) {
        if (request.cantidad() == 0) {
            throw new IllegalArgumentException("La cantidad de ajuste no puede ser cero");
        }

        Producto producto = buscarPorId(id);
        int saldoAnterior = producto.getCantidadStock();
        int saldoNuevo = saldoAnterior + request.cantidad();

        if (saldoNuevo < 0) {
            throw new StockInsuficienteException(
                "El ajuste dejaría el stock en negativo para: " + producto.getNombre());
        }

        producto.setCantidadStock(saldoNuevo);
        productoRepository.save(producto);

        registrarMovimiento(producto, TipoMovimiento.AJUSTE, request.cantidad(),
            saldoAnterior, saldoNuevo, ReferenciaTipo.AJUSTE, null, request.motivo());
        auditLogService.registrar(usuarioActual(), com.cerrajeria.sig.common.audit.AuditAccion.AJUSTE_INVENTARIO,
            "Producto", producto.getId());

        return productoMapper.toResponse(producto);
    }

    /**
     * Descuenta stock por una venta u otra referencia de salida, generando el
     * movimiento de Kardex correspondiente (regla de negocio 2). Reutilizable
     * desde VentaService y OrdenTrabajoService.
     */
    @Transactional
    public Producto descontarStock(Long productoId, int cantidad, ReferenciaTipo referenciaTipo, Long referenciaId) {
        Producto producto = buscarPorId(productoId);
        int saldoAnterior = producto.getCantidadStock();
        int saldoNuevo = saldoAnterior - cantidad;
        if (saldoNuevo < 0) {
            throw new StockInsuficienteException("Stock insuficiente para el producto: " + producto.getNombre());
        }
        producto.setCantidadStock(saldoNuevo);
        productoRepository.save(producto);
        registrarMovimiento(producto, TipoMovimiento.SALIDA, -cantidad, saldoAnterior, saldoNuevo,
            referenciaTipo, referenciaId, null);
        return producto;
    }

    /**
     * Incrementa stock por una compra u otra referencia de entrada, generando
     * el movimiento de Kardex correspondiente. Reutilizable desde CompraService.
     */
    @Transactional
    public Producto incrementarStock(Long productoId, int cantidad, ReferenciaTipo referenciaTipo, Long referenciaId) {
        Producto producto = buscarPorId(productoId);
        int saldoAnterior = producto.getCantidadStock();
        int saldoNuevo = saldoAnterior + cantidad;
        producto.setCantidadStock(saldoNuevo);
        productoRepository.save(producto);
        registrarMovimiento(producto, TipoMovimiento.ENTRADA, cantidad, saldoAnterior, saldoNuevo,
            referenciaTipo, referenciaId, null);
        return producto;
    }

    /** Version publica usada por otros modulos que ya tienen la entidad Producto cargada. */
    /**
     * Guarda la foto de un producto en disco local (ver nota de almacenamiento
     * en application.yml) y actualiza su imagenUrl. RF-16.
     */
    @Transactional
    public ProductoResponse actualizarImagen(Long id, org.springframework.web.multipart.MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen (JPG, PNG, WEBP)");
        }

        Producto producto = buscarPorId(id);

        try {
            java.nio.file.Path carpeta = java.nio.file.Path.of(uploadsDir, "productos");
            java.nio.file.Files.createDirectories(carpeta);

            String extension = switch (contentType) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };
            String nombreArchivo = "producto-" + id + extension;
            java.nio.file.Path destino = carpeta.resolve(nombreArchivo);
            archivo.transferTo(destino);

            // Cache-busting simple: query param con el timestamp para que el navegador
            // no muestre la foto vieja en cache tras reemplazarla.
            producto.setImagenUrl("/uploads/productos/" + nombreArchivo + "?v=" + System.currentTimeMillis());
            productoRepository.save(producto);
        } catch (java.io.IOException ex) {
            throw new RuntimeException("No se pudo guardar la imagen: " + ex.getMessage(), ex);
        }

        return productoMapper.toResponse(producto);
    }

    public Producto buscarEntidadPorId(Long id) {
        return buscarPorId(id);
    }

    private void aplicarCamposComunes(
            Producto producto, String nombre, TipoProducto tipoProducto, String marca,
            Integer stockMinimo, java.math.BigDecimal precioCompra, java.math.BigDecimal precioVenta,
            String imagenUrl, String llavePerfil, String llaveColor, String llaveDisenoTematica,
            String productoModelo, String productoTamano, String productoMaterial
    ) {
        producto.setNombre(nombre);
        producto.setTipoProducto(tipoProducto);
        producto.setMarca(marca);
        if (stockMinimo != null) {
            producto.setStockMinimo(stockMinimo);
        }
        producto.setPrecioCompra(precioCompra);
        producto.setPrecioVenta(precioVenta);
        producto.setImagenUrl(imagenUrl);
        producto.setLlavePerfil(llavePerfil);
        producto.setLlaveColor(llaveColor);
        producto.setLlaveDisenoTematica(llaveDisenoTematica);
        producto.setProductoModelo(productoModelo);
        producto.setProductoTamano(productoTamano);
        producto.setProductoMaterial(productoMaterial);
    }

    private void registrarMovimiento(
            Producto producto, TipoMovimiento tipo, int cantidad, int saldoAnterior, int saldoNuevo,
            ReferenciaTipo referenciaTipo, Long referenciaId, String motivo
    ) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoNuevo(saldoNuevo);
        movimiento.setReferenciaTipo(referenciaTipo);
        movimiento.setReferenciaId(referenciaId);
        movimiento.setMotivo(motivo);
        movimiento.setUsuario(usuarioActual());
        movimientoInventarioRepository.save(movimiento);
    }

    private Usuario usuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + username));
    }

    private Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + id));
    }
}
