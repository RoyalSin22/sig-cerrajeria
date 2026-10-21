package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.inventario.dto.AjusteStockRequest;
import com.cerrajeria.sig.inventario.dto.ImportacionResultado.ErrorFila;
import com.cerrajeria.sig.inventario.dto.ResultadoPlanilla;
import com.cerrajeria.sig.inventario.dto.ResultadoPlanilla.CambioFila;
import com.cerrajeria.sig.inventario.dto.ResultadoPlanilla.CambioPrecio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Planilla de cantidades y precios: se descarga el inventario, se cambia en
 * Excel lo que haga falta y se sube.
 *
 * <p>Cada cambio de stock pasa por {@link ProductoService#ajustarStock} con su
 * motivo, asi queda en el Kardex (regla de negocio 2); los precios, por
 * {@link ProductoService#actualizarPrecios}. Todo ocurre en una transaccion:
 * con un solo error no se cambia nada.
 */
@Service
@RequiredArgsConstructor
public class PlanillaStockService {

    static final String MOTIVO_CONTEO = "Conteo físico (planilla)";
    static final String MOTIVO_ENTRADA = "Mercadería recibida (planilla)";
    private static final int MAX_FILAS = ImportacionProductosService.MAX_FILAS;
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("9999999999.99"); // numeric(12,2)

    /** Excel en Windows abre como ANSI un CSV sin BOM; con BOM ignoraria la linea "sep=". */
    private static final Charset CODIFICACION_EXCEL = Charset.forName("windows-1252");

    private final ProductoService productoService;
    private final ProductoRepository productoRepository;

    /** Que significa el numero escrito en "cantidad". */
    public enum Modo {
        /** Lo que se conto en el estante: reemplaza el stock. */
        CONTEO,
        /** Lo que llego: se suma al stock. */
        ENTRADA
    }

    private enum Precio {
        COMPRA("precio_compra", "compra"),
        VENTA("precio_venta", "venta"),
        DUPLICADO("precio_duplicado", "duplicado");

        final String columna;
        final String etiqueta;

        Precio(String columna, String etiqueta) {
            this.columna = columna;
            this.etiqueta = etiqueta;
        }

        BigDecimal de(Producto p) {
            return switch (this) {
                case COMPRA -> p.getPrecioCompra();
                case VENTA -> p.getPrecioVenta();
                case DUPLICADO -> p.getPrecioDuplicado();
            };
        }
    }

    /**
     * Todo el catalogo activo, con "cantidad" vacia y los precios actuales.
     * "sep=;" hace que Excel lo abra en columnas con cualquier configuracion
     * regional. Codigo y precios van como ="..." (texto): asi Excel no los
     * convierte en numero o fecha ("001" → 1, "3-12" → 3 de diciembre, "1.20"
     * → fecha en una PC con coma decimal).
     */
    @Transactional(readOnly = true)
    public byte[] exportar() {
        StringBuilder csv = new StringBuilder(
            "sep=;\r\ncodigo;nombre;tipo;marca;stock_actual;cantidad;precio_compra;precio_venta;precio_duplicado\r\n");
        for (Producto p : productoRepository.findByActivoTrueOrderByTipoProductoAscNombreAsc()) {
            csv.append(comoTexto(p.getCodigo())).append(';')
                .append(celda(p.getNombre())).append(';')
                .append(p.getTipoProducto().name().toLowerCase(Locale.ROOT).replace('_', ' ')).append(';')
                .append(celda(p.getMarca())).append(';')
                .append(p.getCantidadStock()).append(";;")
                .append(precioTexto(p.getPrecioCompra())).append(';')
                .append(precioTexto(p.getPrecioVenta())).append(';')
                .append(precioTexto(p.getPrecioDuplicado())).append("\r\n");
        }
        return csv.toString().getBytes(CODIFICACION_EXCEL);
    }

    private static String comoTexto(String valor) {
        return "=\"" + valor.replace("\"", "\"\"") + '"';
    }

    private static String precioTexto(BigDecimal precio) {
        return precio == null ? "" : comoTexto(precio.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    private static String celda(String texto) {
        if (texto == null) return "";
        // Una celda que empieza con = + - @ Excel la ejecuta como formula
        String seguro = texto.matches("^[=+\\-@].*") ? "'" + texto : texto;
        if (seguro.contains(";") || seguro.contains("\"") || seguro.contains("\n") || seguro.contains("\r")) {
            return '"' + seguro.replace("\"", "\"\"") + '"';
        }
        return seguro;
    }

    /**
     * Con {@code confirmar = false} solo revisa (no cambia nada); con
     * {@code true} aplica los cambios, siempre que no haya ningun error. Una
     * celda vacia no cambia nada; el modo solo afecta a "cantidad".
     */
    @Transactional
    public ResultadoPlanilla actualizar(byte[] archivo, Modo modo, boolean confirmar) {
        if (archivo == null || archivo.length == 0) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        LectorCsv.rechazarLibroExcel(archivo);

        List<List<String>> registros = LectorCsv.leer(archivo);
        int indiceCabecera = 0;
        while (indiceCabecera < registros.size() && esVacia(registros.get(indiceCabecera))) {
            indiceCabecera++;
        }
        if (indiceCabecera == registros.size()) {
            throw new IllegalArgumentException("El archivo no tiene datos");
        }

        List<ErrorFila> errores = new ArrayList<>();
        Integer colCodigo = null;
        Integer colCantidad = null;
        Map<Precio, Integer> colPrecios = new EnumMap<>(Precio.class);
        List<String> cabecera = registros.get(indiceCabecera);
        for (int i = 0; i < cabecera.size(); i++) {
            String nombre = ImportacionProductosService.normalizar(cabecera.get(i).trim());
            if ((nombre.equals("codigo") || nombre.equals("cod")) && colCodigo == null) colCodigo = i;
            if (nombre.equals("cantidad") && colCantidad == null) colCantidad = i;
            for (Precio p : Precio.values()) {
                if (nombre.equals(p.columna)) colPrecios.putIfAbsent(p, i);
            }
        }
        if (colCodigo == null) errores.add(new ErrorFila(1, "Falta la columna \"codigo\""));
        if (colCantidad == null && colPrecios.isEmpty()) {
            errores.add(new ErrorFila(1, "Falta la columna \"cantidad\" (o alguna de precios)"));
        }
        if (!errores.isEmpty()) {
            return new ResultadoPlanilla(0, 0, 0, List.of(), errores);
        }

        // Solo las filas con algo escrito en cantidad o en algun precio
        Map<Integer, List<String>> filas = new LinkedHashMap<>();
        for (int i = indiceCabecera + 1; i < registros.size(); i++) {
            List<String> r = registros.get(i);
            boolean conDatos = (colCantidad != null && !celda(r, colCantidad).isEmpty())
                || colPrecios.values().stream().anyMatch(c -> !celda(r, c).isEmpty());
            if (conDatos) filas.put(i + 1, r);
        }
        if (filas.size() > MAX_FILAS) {
            throw new IllegalArgumentException("La planilla tiene " + filas.size()
                + " filas con datos; el máximo es " + MAX_FILAS + ". Divídela en varios archivos.");
        }

        final int columnaCodigo = colCodigo;
        Map<String, Producto> porCodigo = productoRepository.findByCodigoIn(
                filas.values().stream().map(r -> limpiar(celda(r, columnaCodigo))).filter(c -> !c.isEmpty())
                    .collect(Collectors.toSet()))
            .stream().collect(Collectors.toMap(Producto::getCodigo, Function.identity()));

        Map<String, Integer> filaPorCodigo = new LinkedHashMap<>();
        List<CambioFila> cambios = new ArrayList<>();
        int sinCambio = 0;
        for (Map.Entry<Integer, List<String>> e : filas.entrySet()) {
            int fila = e.getKey();
            List<String> r = e.getValue();
            String codigo = limpiar(celda(r, colCodigo));
            if (codigo.isEmpty()) {
                errores.add(new ErrorFila(fila, "Tiene datos pero no tiene código"));
                continue;
            }
            Integer filaPrevia = filaPorCodigo.putIfAbsent(codigo, fila);
            if (filaPrevia != null) {
                errores.add(new ErrorFila(fila, "El código " + codigo + " está repetido (ya está en la fila "
                    + filaPrevia + ")"));
                continue;
            }
            Producto producto = porCodigo.get(codigo);
            if (producto == null || !Boolean.TRUE.equals(producto.getActivo())) {
                errores.add(new ErrorFila(fila, "El código " + codigo + " no está en el inventario"));
                continue;
            }

            int erroresAntes = errores.size();
            Integer stockAnterior = null;
            Integer stockNuevo = null;
            String textoCantidad = colCantidad == null ? "" : celda(r, colCantidad);
            if (!textoCantidad.isEmpty()) {
                Integer cantidad = cantidad(textoCantidad);
                if (cantidad == null) {
                    errores.add(new ErrorFila(fila, "La cantidad debe ser un número entero sin signo: " + textoCantidad));
                } else {
                    int anterior = producto.getCantidadStock();
                    int nuevo = modo == Modo.CONTEO ? cantidad : anterior + cantidad;
                    if (nuevo != anterior) {
                        stockAnterior = anterior;
                        stockNuevo = nuevo;
                    }
                }
            }

            List<CambioPrecio> precios = new ArrayList<>();
            for (Map.Entry<Precio, Integer> c : colPrecios.entrySet()) {
                String texto = limpiar(celda(r, c.getValue()));
                if (texto.isEmpty()) continue;
                BigDecimal nuevo = precio(texto);
                if (nuevo == null) {
                    errores.add(new ErrorFila(fila, "\"" + c.getKey().columna + "\" no es un precio válido: " + texto));
                    continue;
                }
                BigDecimal anterior = c.getKey().de(producto);
                if (anterior != null && anterior.compareTo(nuevo) == 0) continue;
                if (c.getKey() == Precio.DUPLICADO && !producto.getTipoProducto().esLlave()) {
                    errores.add(new ErrorFila(fila, producto.getNombre() + " no es una llave: no lleva precio_duplicado"));
                    continue;
                }
                precios.add(new CambioPrecio(c.getKey().etiqueta, anterior, nuevo));
            }

            if (errores.size() > erroresAntes) continue;
            if (stockNuevo == null && precios.isEmpty()) {
                sinCambio++;
            } else {
                cambios.add(new CambioFila(fila, codigo, producto.getNombre(), stockAnterior, stockNuevo, precios));
            }
        }

        if (filas.isEmpty()) {
            errores.add(new ErrorFila(0, "Ninguna fila tiene la cantidad o un precio lleno"));
        }

        int actualizados = 0;
        if (confirmar && errores.isEmpty()) {
            String motivo = modo == Modo.CONTEO ? MOTIVO_CONTEO : MOTIVO_ENTRADA;
            for (CambioFila c : cambios) {
                Producto p = porCodigo.get(c.codigo());
                if (c.stockNuevo() != null) {
                    productoService.ajustarStock(p.getId(),
                        new AjusteStockRequest(c.stockNuevo() - p.getCantidadStock(), motivo));
                }
                if (!c.precios().isEmpty()) {
                    Map<String, BigDecimal> nuevos = c.precios().stream()
                        .collect(Collectors.toMap(CambioPrecio::precio, CambioPrecio::nuevo));
                    productoService.actualizarPrecios(p.getId(), nuevos.get(Precio.COMPRA.etiqueta),
                        nuevos.get(Precio.VENTA.etiqueta), nuevos.get(Precio.DUPLICADO.etiqueta));
                }
                actualizados++;
            }
        }
        return new ResultadoPlanilla(filas.size(), actualizados, sinCambio, cambios, errores);
    }

    private static String celda(List<String> registro, int indice) {
        return indice < registro.size() ? registro.get(indice).trim() : "";
    }

    /** Si el archivo no paso por Excel, el texto llega tal cual se exporto: ="KW1-001" → =KW1-001. */
    static String limpiar(String valor) {
        return valor.startsWith("=") ? valor.substring(1).trim() : valor;
    }

    /**
     * Entero sin signo. "1,000" y "1.000" son mil (separador de miles, segun la
     * PC), no uno: una cantidad de llaves nunca lleva decimales.
     */
    static Integer cantidad(String texto) {
        String s = texto.replaceAll("[\\s\\u00A0]", "");
        if (s.matches("\\d{1,3}([.,]\\d{3})+")) {
            s = s.replaceAll("[.,]", "");
        }
        BigDecimal numero = ImportacionProductosService.numero(s);
        if (numero == null || numero.signum() < 0) return null;
        try {
            return numero.intValueExact();
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    /** "2.50", "2,50" o "S/ 2.50"; sin signo y con hasta 2 decimales. */
    static BigDecimal precio(String texto) {
        BigDecimal numero = ImportacionProductosService.numero(texto);
        if (numero == null || numero.signum() < 0) return null;
        numero = numero.setScale(2, RoundingMode.HALF_UP);
        return numero.compareTo(PRECIO_MAXIMO) > 0 ? null : numero;
    }

    private static boolean esVacia(List<String> celdas) {
        return celdas.stream().allMatch(c -> c.trim().isEmpty());
    }
}
