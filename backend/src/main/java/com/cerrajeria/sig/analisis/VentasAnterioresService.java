package com.cerrajeria.sig.analisis;

import com.cerrajeria.sig.analisis.dto.ResultadoVentasAnteriores;
import com.cerrajeria.sig.analisis.dto.ResultadoVentasAnteriores.Linea;
import com.cerrajeria.sig.analisis.dto.ResumenVentasAnteriores;
import com.cerrajeria.sig.common.audit.AuditAccion;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.audit.DetalleAuditoria;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.inventario.ImportacionProductosService;
import com.cerrajeria.sig.inventario.LectorCsv;
import com.cerrajeria.sig.inventario.Producto;
import com.cerrajeria.sig.inventario.ProductoRepository;
import com.cerrajeria.sig.inventario.dto.ImportacionResultado.ErrorFila;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ventas de antes del sistema (un cuaderno pasado a Excel) para el historial de
 * Power BI. Se guardan aparte de las ventas: no tocan la caja ni el stock, que
 * ya se contaron al empezar con el sistema. Columnas: fecha, producto, cantidad
 * y precio (o total), y opcionalmente codigo y metodo de pago.
 */
@Service
@RequiredArgsConstructor
public class VentasAnterioresService {

    static final int MAX_FILAS = 20000;
    private static final int MUESTRA = 30;
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("9999999999.99"); // numeric(12,2)
    private static final LocalDate PRIMERA_FECHA = LocalDate.of(2000, 1, 1);

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
        DateTimeFormatter.ofPattern("d/M/uuuu"), DateTimeFormatter.ofPattern("d-M-uuuu"),
        DateTimeFormatter.ofPattern("d.M.uuuu"), DateTimeFormatter.ofPattern("uuuu-M-d"),
        DateTimeFormatter.ofPattern("d/M/uu")).stream()
        .map(f -> f.withResolverStyle(ResolverStyle.STRICT)).toList();

    private enum Columna { FECHA, PRODUCTO, CODIGO, CANTIDAD, PRECIO, TOTAL, METODO }

    private static final Map<String, Columna> ALIAS = new HashMap<>();

    static {
        for (String a : List.of("fecha", "dia", "fecha_venta")) ALIAS.put(a, Columna.FECHA);
        for (String a : List.of("producto", "descripcion", "detalle", "articulo", "nombre", "item", "concepto"))
            ALIAS.put(a, Columna.PRODUCTO);
        for (String a : List.of("codigo", "cod")) ALIAS.put(a, Columna.CODIGO);
        for (String a : List.of("cantidad", "cant", "unidades")) ALIAS.put(a, Columna.CANTIDAD);
        for (String a : List.of("precio", "precio_unitario", "p_unitario", "precio_venta", "pu"))
            ALIAS.put(a, Columna.PRECIO);
        for (String a : List.of("total", "importe", "monto", "subtotal")) ALIAS.put(a, Columna.TOTAL);
        for (String a : List.of("metodo", "metodo_pago", "pago", "forma_pago", "medio_pago")) ALIAS.put(a, Columna.METODO);
    }

    private final VentaAnteriorRepository ventaAnteriorRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public ResumenVentasAnteriores resumen() {
        var r = ventaAnteriorRepository.resumen();
        return new ResumenVentasAnteriores(r.getFilas(), r.getTotal(), r.getDesde(), r.getHasta());
    }

    @Transactional
    public ResultadoVentasAnteriores importar(byte[] archivo, boolean confirmar) {
        if (archivo == null || archivo.length == 0) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        List<List<String>> registros = LectorCsv.leer(archivo);

        int cabecera = 0;
        while (cabecera < registros.size() && esVacia(registros.get(cabecera))) cabecera++;
        if (cabecera == registros.size()) {
            throw new IllegalArgumentException("El archivo no tiene datos");
        }

        List<ErrorFila> errores = new ArrayList<>();
        List<String> ignoradas = new ArrayList<>();
        Map<Columna, Integer> posiciones = new EnumMap<>(Columna.class);
        List<String> titulos = registros.get(cabecera);
        for (int i = 0; i < titulos.size(); i++) {
            String original = titulos.get(i).trim();
            if (original.isEmpty()) continue;
            Columna col = ALIAS.get(normalizar(original));
            if (col == null || posiciones.containsKey(col)) {
                ignoradas.add(original);
            } else {
                posiciones.put(col, i);
            }
        }
        for (Columna obligatoria : List.of(Columna.FECHA, Columna.PRODUCTO)) {
            if (!posiciones.containsKey(obligatoria)) {
                errores.add(new ErrorFila(0, "Falta la columna \"" + obligatoria.name().toLowerCase(Locale.ROOT) + "\""));
            }
        }
        if (!posiciones.containsKey(Columna.PRECIO) && !posiciones.containsKey(Columna.TOTAL)) {
            errores.add(new ErrorFila(0, "Falta la columna \"precio\" (o \"total\")"));
        }
        if (!errores.isEmpty()) {
            return new ResultadoVentasAnteriores(0, 0, 0, BigDecimal.ZERO, null, null, List.of(), errores, ignoradas);
        }

        int datos = (int) registros.stream().skip(cabecera + 1L).filter(r -> !esVacia(r)).count();
        if (datos > MAX_FILAS) {
            throw new IllegalArgumentException(
                "El archivo tiene " + datos + " ventas; el máximo por carga es " + MAX_FILAS + ". Divídelo en partes.");
        }

        Catalogo catalogo = new Catalogo(productoRepository.findAll());
        Usuario usuario = usuarioActual();
        List<VentaAnterior> ventas = new ArrayList<>();
        List<Linea> muestra = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        LocalDate desde = null;
        LocalDate hasta = null;
        int enlazadas = 0;

        for (int i = cabecera + 1; i < registros.size(); i++) {
            List<String> celdas = registros.get(i);
            if (esVacia(celdas)) continue;
            int fila = i + 1;
            List<String> problemas = new ArrayList<>();
            VentaAnterior v = leerFila(celdas, posiciones, catalogo, problemas);
            if (!problemas.isEmpty()) {
                problemas.forEach(p -> errores.add(new ErrorFila(fila, p)));
                continue;
            }
            v.setUsuario(usuario);
            ventas.add(v);
            if (v.getProducto() != null) enlazadas++;
            total = total.add(v.getPrecioUnitario().multiply(BigDecimal.valueOf(v.getCantidad())));
            desde = desde == null || v.getFecha().isBefore(desde) ? v.getFecha() : desde;
            hasta = hasta == null || v.getFecha().isAfter(hasta) ? v.getFecha() : hasta;
            if (muestra.size() < MUESTRA) {
                muestra.add(new Linea(fila, v.getFecha(),
                    v.getProducto() != null ? v.getProducto().getNombre() : v.getDescripcion(),
                    v.getProducto() != null, v.getCantidad(), v.getPrecioUnitario(), v.getMetodoPago()));
            }
        }
        if (ventas.isEmpty() && errores.isEmpty()) {
            throw new IllegalArgumentException("El archivo no tiene ventas debajo de la cabecera");
        }

        int cargadas = 0;
        if (confirmar && errores.isEmpty()) {
            ventaAnteriorRepository.saveAll(ventas);
            cargadas = ventas.size();
            auditLogService.registrar(usuario, AuditAccion.CREAR, "VentasAnteriores", (long) cargadas,
                DetalleAuditoria.de("Ventas anteriores cargadas desde archivo: " + cargadas + " líneas, del "
                    + desde + " al " + hasta + ", por S/ " + total.setScale(2, RoundingMode.HALF_UP)));
        }
        return new ResultadoVentasAnteriores(datos, cargadas, enlazadas, total.setScale(2, RoundingMode.HALF_UP), desde, hasta, muestra, errores, ignoradas);
    }

    /** Borra todas para volver a subir el archivo corregido. */
    @Transactional
    public long borrarTodas() {
        long filas = ventaAnteriorRepository.count();
        if (filas > 0) {
            ventaAnteriorRepository.deleteAllInBatch();
            auditLogService.registrar(usuarioActual(), AuditAccion.ELIMINAR, "VentasAnteriores", filas,
                DetalleAuditoria.de("Se borraron las " + filas + " ventas anteriores cargadas desde archivo"));
        }
        return filas;
    }

    private static VentaAnterior leerFila(List<String> celdas, Map<Columna, Integer> pos, Catalogo catalogo,
                                          List<String> problemas) {
        VentaAnterior v = new VentaAnterior();

        String textoFecha = celda(celdas, pos, Columna.FECHA);
        LocalDate fecha = textoFecha == null ? null : fecha(textoFecha);
        if (textoFecha == null) {
            problemas.add("Falta la fecha");
        } else if (fecha == null) {
            problemas.add("Fecha no válida: \"" + textoFecha + "\" (usa día/mes/año, por ejemplo 15/03/2025)");
        } else if (fecha.isAfter(LocalDate.now()) || fecha.isBefore(PRIMERA_FECHA)) {
            problemas.add("Fecha fuera de rango: " + textoFecha);
        }
        v.setFecha(fecha);

        String producto = celda(celdas, pos, Columna.PRODUCTO);
        String codigo = celda(celdas, pos, Columna.CODIGO);
        if (producto == null && codigo == null) {
            problemas.add("Falta el producto");
        }
        Producto enlazado = catalogo.buscar(codigo, producto);
        v.setProducto(enlazado);
        String descripcion = producto != null ? producto : enlazado != null ? enlazado.getNombre() : codigo;
        if (descripcion != null && descripcion.length() > 150) {
            problemas.add("El producto tiene más de 150 letras");
        }
        v.setDescripcion(descripcion);

        int cantidad = 1;
        String textoCantidad = celda(celdas, pos, Columna.CANTIDAD);
        if (textoCantidad != null) {
            BigDecimal n = ImportacionProductosService.numero(textoCantidad);
            if (n == null || n.signum() <= 0 || n.stripTrailingZeros().scale() > 0 || n.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
                problemas.add("Cantidad no válida: \"" + textoCantidad + "\" (debe ser un número entero mayor que 0)");
            } else {
                cantidad = n.intValueExact();
            }
        }
        v.setCantidad(cantidad);

        BigDecimal precio = monto(celda(celdas, pos, Columna.PRECIO), "precio", problemas);
        if (precio == null) {
            BigDecimal total = monto(celda(celdas, pos, Columna.TOTAL), "total", problemas);
            if (total != null) {
                precio = total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
            }
        }
        if (precio == null && problemas.isEmpty()) {
            problemas.add("Falta el precio (o el total)");
        }
        v.setPrecioUnitario(precio == null ? null : precio.setScale(2, RoundingMode.HALF_UP));

        String textoMetodo = celda(celdas, pos, Columna.METODO);
        if (textoMetodo != null) {
            String metodo = switch (normalizar(textoMetodo)) {
                case "efectivo", "contado", "cash" -> "EFECTIVO";
                case "yape" -> "YAPE";
                case "plin" -> "PLIN";
                case "tarjeta", "pos", "visa" -> "TARJETA";
                default -> null;
            };
            if (metodo == null) {
                problemas.add("Método de pago desconocido: \"" + textoMetodo + "\" (efectivo, yape, plin o tarjeta)");
            }
            v.setMetodoPago(metodo);
        }
        return v;
    }

    private static BigDecimal monto(String texto, String campo, List<String> problemas) {
        if (texto == null) return null;
        BigDecimal n = ImportacionProductosService.numero(texto);
        if (n == null || n.signum() < 0 || n.compareTo(PRECIO_MAXIMO) > 0) {
            problemas.add("\"" + campo + "\" no es un monto válido: " + texto);
            return null;
        }
        return n;
    }

    static LocalDate fecha(String texto) {
        String t = texto.trim();
        // Excel a veces guarda "15/03/2025 00:00"
        int espacio = t.indexOf(' ');
        if (espacio > 0) t = t.substring(0, espacio);
        for (DateTimeFormatter f : FORMATOS_FECHA) {
            try {
                LocalDate d = LocalDate.parse(t, f);
                // "d/M/uu" lee "25" como 2025, nunca 1925
                return d.getYear() < 100 ? d.plusYears(2000) : d;
            } catch (DateTimeParseException ignorada) {
                // probar el siguiente formato
            }
        }
        return null;
    }

    private static String celda(List<String> celdas, Map<Columna, Integer> pos, Columna col) {
        Integer i = pos.get(col);
        if (i == null || i >= celdas.size()) return null;
        String v = celdas.get(i).trim();
        return v.isEmpty() ? null : v;
    }

    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "").replaceAll("_de_", "_");
    }

    private static boolean esVacia(List<String> celdas) {
        return celdas.stream().allMatch(c -> c.trim().isEmpty());
    }

    private Usuario usuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    /** Enlaza por codigo, o por nombre si es unico (sin tildes ni mayusculas). */
    private static final class Catalogo {
        private final Map<String, Producto> porCodigo = new HashMap<>();
        private final Map<String, Producto> porNombre = new HashMap<>();

        Catalogo(List<Producto> productos) {
            Map<String, Integer> repetidos = new HashMap<>();
            for (Producto p : productos) {
                porCodigo.put(p.getCodigo().toUpperCase(Locale.ROOT), p);
                String n = normalizar(p.getNombre());
                repetidos.merge(n, 1, Integer::sum);
                porNombre.put(n, p);
            }
            repetidos.forEach((n, veces) -> {
                if (veces > 1) porNombre.remove(n);
            });
        }

        Producto buscar(String codigo, String nombre) {
            if (codigo != null) {
                Producto p = porCodigo.get(codigo.toUpperCase(Locale.ROOT));
                if (p != null) return p;
            }
            return nombre == null ? null : porNombre.get(normalizar(nombre));
        }
    }
}
