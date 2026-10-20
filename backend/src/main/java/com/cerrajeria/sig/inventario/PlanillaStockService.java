package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.inventario.dto.ActualizacionStockResultado;
import com.cerrajeria.sig.inventario.dto.ActualizacionStockResultado.CambioStock;
import com.cerrajeria.sig.inventario.dto.AjusteStockRequest;
import com.cerrajeria.sig.inventario.dto.ImportacionResultado.ErrorFila;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Planilla de cantidades: se descarga el inventario, se llena la columna
 * "cantidad" en Excel y se sube.
 *
 * <p>Cada cambio pasa por {@link ProductoService#ajustarStock} con su motivo,
 * asi queda en el Kardex (regla de negocio 2). Todo ocurre en una transaccion:
 * con un solo error no se cambia ningun stock.
 */
@Service
@RequiredArgsConstructor
public class PlanillaStockService {

    static final String MOTIVO_CONTEO = "Conteo físico (planilla)";
    static final String MOTIVO_ENTRADA = "Mercadería recibida (planilla)";
    private static final int MAX_FILAS = ImportacionProductosService.MAX_FILAS;

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

    /**
     * Todo el catalogo activo, con "cantidad" vacia para llenar. "sep=;" hace
     * que Excel lo abra en columnas con cualquier configuracion regional, y el
     * codigo va como ="..." para que Excel no lo convierta en numero o fecha
     * ("001" → 1, "3-12" → 3 de diciembre) y despues no se encuentre.
     */
    @Transactional(readOnly = true)
    public byte[] exportar() {
        StringBuilder csv = new StringBuilder("sep=;\r\ncodigo;nombre;tipo;marca;stock_actual;cantidad\r\n");
        for (Producto p : productoRepository.findByActivoTrueOrderByTipoProductoAscNombreAsc()) {
            csv.append("=\"").append(p.getCodigo().replace("\"", "\"\"")).append('"').append(';')
                .append(celda(p.getNombre())).append(';')
                .append(p.getTipoProducto().name().toLowerCase(Locale.ROOT).replace('_', ' ')).append(';')
                .append(celda(p.getMarca())).append(';')
                .append(p.getCantidadStock()).append(";\r\n");
        }
        return csv.toString().getBytes(CODIFICACION_EXCEL);
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
     * {@code true} aplica los cambios, siempre que no haya ningun error.
     */
    @Transactional
    public ActualizacionStockResultado actualizar(byte[] archivo, Modo modo, boolean confirmar) {
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
        List<String> cabecera = registros.get(indiceCabecera);
        for (int i = 0; i < cabecera.size(); i++) {
            String nombre = ImportacionProductosService.normalizar(cabecera.get(i).trim());
            if ((nombre.equals("codigo") || nombre.equals("cod")) && colCodigo == null) colCodigo = i;
            if (nombre.equals("cantidad") && colCantidad == null) colCantidad = i;
        }
        if (colCodigo == null) errores.add(new ErrorFila(1, "Falta la columna \"codigo\""));
        if (colCantidad == null) errores.add(new ErrorFila(1, "Falta la columna \"cantidad\""));
        if (!errores.isEmpty()) {
            return new ActualizacionStockResultado(0, 0, 0, List.of(), errores);
        }

        // Fila de Excel → [codigo, cantidad], solo las que tienen cantidad
        Map<Integer, String[]> filas = new LinkedHashMap<>();
        for (int i = indiceCabecera + 1; i < registros.size(); i++) {
            List<String> r = registros.get(i);
            String cantidad = celda(r, colCantidad);
            if (!cantidad.isEmpty()) {
                filas.put(i + 1, new String[]{limpiarCodigo(celda(r, colCodigo)), cantidad});
            }
        }
        if (filas.size() > MAX_FILAS) {
            throw new IllegalArgumentException("La planilla tiene " + filas.size()
                + " filas con cantidad; el máximo es " + MAX_FILAS + ". Divídela en varios archivos.");
        }

        Map<String, Producto> porCodigo = productoRepository.findByCodigoIn(
                filas.values().stream().map(f -> f[0]).filter(c -> !c.isEmpty()).collect(Collectors.toSet()))
            .stream().collect(Collectors.toMap(Producto::getCodigo, Function.identity()));

        Map<String, Integer> filaPorCodigo = new LinkedHashMap<>();
        List<CambioStock> cambios = new ArrayList<>();
        int sinCambio = 0;
        for (Map.Entry<Integer, String[]> e : filas.entrySet()) {
            int fila = e.getKey();
            String codigo = e.getValue()[0];
            if (codigo.isEmpty()) {
                errores.add(new ErrorFila(fila, "Tiene cantidad pero no tiene código"));
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
            Integer cantidad = cantidad(e.getValue()[1]);
            if (cantidad == null) {
                errores.add(new ErrorFila(fila, "La cantidad debe ser un número entero sin signo: " + e.getValue()[1]));
                continue;
            }
            int anterior = producto.getCantidadStock();
            int nuevo = modo == Modo.CONTEO ? cantidad : anterior + cantidad;
            if (nuevo == anterior) {
                sinCambio++;
            } else {
                cambios.add(new CambioStock(fila, codigo, producto.getNombre(), anterior, nuevo));
            }
        }

        if (filas.isEmpty()) {
            errores.add(new ErrorFila(0, "Ninguna fila tiene la columna \"cantidad\" llena"));
        }

        int actualizados = 0;
        if (confirmar && errores.isEmpty()) {
            String motivo = modo == Modo.CONTEO ? MOTIVO_CONTEO : MOTIVO_ENTRADA;
            for (CambioStock c : cambios) {
                Producto p = porCodigo.get(c.codigo());
                productoService.ajustarStock(p.getId(),
                    new AjusteStockRequest(c.stockNuevo() - p.getCantidadStock(), motivo));
                actualizados++;
            }
        }
        return new ActualizacionStockResultado(filas.size(), actualizados, sinCambio, cambios, errores);
    }

    private static String celda(List<String> registro, int indice) {
        return indice < registro.size() ? registro.get(indice).trim() : "";
    }

    /** Si el archivo no paso por Excel, el codigo llega tal cual se exporto: ="KW1-001" → =KW1-001. */
    static String limpiarCodigo(String codigo) {
        return codigo.startsWith("=") ? codigo.substring(1).trim() : codigo;
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

    private static boolean esVacia(List<String> celdas) {
        return celdas.stream().allMatch(c -> c.trim().isEmpty());
    }
}
