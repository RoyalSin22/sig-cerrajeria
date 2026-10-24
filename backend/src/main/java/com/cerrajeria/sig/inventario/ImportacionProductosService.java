package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.inventario.dto.ImportacionResultado;
import com.cerrajeria.sig.inventario.dto.ImportacionResultado.ErrorFila;
import com.cerrajeria.sig.inventario.dto.ImportacionResultado.FilaProducto;
import com.cerrajeria.sig.inventario.dto.ProductoCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Carga inicial (o masiva) del inventario desde un CSV exportado de Excel.
 *
 * <p>Cada producto se crea con {@link ProductoService#crear}, el mismo camino
 * que el formulario: el stock inicial queda registrado en el Kardex (regla de
 * negocio 2) con su propio motivo. Todo ocurre en una sola transaccion: si un
 * producto falla, no se crea ninguno.
 *
 * <p>Solo da de alta productos nuevos. Un codigo que ya existe es un error, no
 * una actualizacion: cambiar stock o precios de lo que ya esta cargado se hace
 * desde la ficha del producto (el ajuste de stock exige un motivo).
 */
@Service
@RequiredArgsConstructor
public class ImportacionProductosService {

    static final String MOTIVO_STOCK_IMPORTADO = "Stock inicial importado desde archivo";
    static final int MAX_FILAS = 5000;
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("9999999999.99"); // numeric(12,2)

    private final ProductoService productoService;
    private final ProductoRepository productoRepository;

    /** Columnas reconocidas, con el largo maximo de su columna en la base (0 = no es texto libre). */
    private enum Columna {
        CODIGO(50, true), NOMBRE(150, true), TIPO(0, true), MARCA(100, false),
        STOCK(0, false), STOCK_MINIMO(0, false), PRECIO_COMPRA(0, true), PRECIO_VENTA(0, true),
        PERFIL(50, false), COLOR(30, false), DISENO(50, false), MODELO(100, false),
        TAMANO(30, false), MATERIAL(50, false), PRECIO_DUPLICADO(0, false);

        final int largoMaximo;
        final boolean obligatoria;

        Columna(int largoMaximo, boolean obligatoria) {
            this.largoMaximo = largoMaximo;
            this.obligatoria = obligatoria;
        }

        String nombreEnArchivo() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Variantes de cabecera aceptadas (ya normalizadas: sin tildes, minusculas, "_"). */
    private static final Map<String, Columna> ALIAS_COLUMNAS = new HashMap<>();

    static {
        for (Columna c : Columna.values()) {
            ALIAS_COLUMNAS.put(c.nombreEnArchivo(), c);
        }
        ALIAS_COLUMNAS.put("cod", Columna.CODIGO);
        ALIAS_COLUMNAS.put("producto", Columna.NOMBRE);
        ALIAS_COLUMNAS.put("descripcion", Columna.NOMBRE);
        ALIAS_COLUMNAS.put("tipo_producto", Columna.TIPO);
        ALIAS_COLUMNAS.put("categoria", Columna.TIPO);
        ALIAS_COLUMNAS.put("cantidad", Columna.STOCK);
        ALIAS_COLUMNAS.put("stock_inicial", Columna.STOCK);
        ALIAS_COLUMNAS.put("cantidad_stock", Columna.STOCK);
        ALIAS_COLUMNAS.put("stock_min", Columna.STOCK_MINIMO);
        ALIAS_COLUMNAS.put("minimo", Columna.STOCK_MINIMO);
        ALIAS_COLUMNAS.put("costo", Columna.PRECIO_COMPRA);
        ALIAS_COLUMNAS.put("precio_costo", Columna.PRECIO_COMPRA);
        ALIAS_COLUMNAS.put("precio", Columna.PRECIO_VENTA);
        ALIAS_COLUMNAS.put("duplicado", Columna.PRECIO_DUPLICADO);
        ALIAS_COLUMNAS.put("llave_perfil", Columna.PERFIL);
        ALIAS_COLUMNAS.put("llave_color", Columna.COLOR);
        ALIAS_COLUMNAS.put("tematica", Columna.DISENO);
        ALIAS_COLUMNAS.put("tamanio", Columna.TAMANO);
    }

    /** Nombres de tipo aceptados (normalizados), ademas del nombre del enum. */
    private static final Map<String, TipoProducto> ALIAS_TIPOS = new HashMap<>();

    static {
        for (TipoProducto t : TipoProducto.values()) {
            ALIAS_TIPOS.put(t.name().toLowerCase(Locale.ROOT), t);
        }
        ALIAS_TIPOS.put("llave_de_color", TipoProducto.LLAVE_COLOR);
        ALIAS_TIPOS.put("llave_de_diseno", TipoProducto.LLAVE_DISENO);
        ALIAS_TIPOS.put("llave", TipoProducto.LLAVE_VIRGEN);
        ALIAS_TIPOS.put("llaves", TipoProducto.LLAVE_VIRGEN);
        ALIAS_TIPOS.put("chapas", TipoProducto.CHAPA);
        ALIAS_TIPOS.put("cerradura", TipoProducto.CHAPA);
        ALIAS_TIPOS.put("cilindros", TipoProducto.CILINDRO);
        ALIAS_TIPOS.put("candados", TipoProducto.CANDADO);
        ALIAS_TIPOS.put("llaveros", TipoProducto.LLAVERO);
        ALIAS_TIPOS.put("argollas", TipoProducto.ARGOLLA);
        ALIAS_TIPOS.put("accesorios", TipoProducto.ACCESORIO);
        ALIAS_TIPOS.put("repuesto", TipoProducto.REPUESTO_LICUADORA);
        ALIAS_TIPOS.put("repuestos", TipoProducto.REPUESTO_LICUADORA);
        ALIAS_TIPOS.put("repuesto_de_licuadora", TipoProducto.REPUESTO_LICUADORA);
        ALIAS_TIPOS.put("licuadora", TipoProducto.REPUESTO_LICUADORA);
    }

    /**
     * Lee el archivo y valida todas las filas. Con {@code confirmar = false} es
     * solo una revision (no guarda nada); con {@code true} crea los productos,
     * siempre que no haya ningun error.
     */
    @Transactional
    public ImportacionResultado importar(byte[] archivo, boolean confirmar) {
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
        List<String> ignoradas = new ArrayList<>();
        Map<Columna, Integer> posiciones = leerCabecera(registros.get(indiceCabecera), errores, ignoradas);
        if (!errores.isEmpty()) {
            return new ImportacionResultado(0, 0, List.of(), errores, ignoradas);
        }

        List<FilaLeida> filas = new ArrayList<>();
        for (int i = indiceCabecera + 1; i < registros.size(); i++) {
            if (!esVacia(registros.get(i))) {
                filas.add(new FilaLeida(i + 1, registros.get(i)));
            }
        }
        if (filas.size() > MAX_FILAS) {
            throw new IllegalArgumentException(
                "El archivo tiene " + filas.size() + " productos; el máximo por importación es " + MAX_FILAS
                    + ". Divídelo en varios archivos.");
        }

        List<ProductoCreateRequest> solicitudes = new ArrayList<>();
        List<FilaProducto> validas = new ArrayList<>();
        Map<String, Integer> filaPorCodigo = new LinkedHashMap<>();

        for (FilaLeida fila : filas) {
            List<String> problemas = new ArrayList<>();
            String codigo = fila.celda(posiciones.get(Columna.CODIGO));
            if (!codigo.isEmpty()) {
                Integer filaPrevia = filaPorCodigo.putIfAbsent(codigo, fila.numero());
                if (filaPrevia != null) {
                    problemas.add("El código " + codigo + " está repetido en el archivo (ya está en la fila "
                        + filaPrevia + ")");
                }
            }
            ProductoCreateRequest req = convertir(fila, posiciones, problemas);
            if (problemas.isEmpty()) {
                solicitudes.add(req);
                validas.add(new FilaProducto(fila.numero(), req.codigo(), req.nombre(), req.tipoProducto(),
                    req.cantidadStock(), req.precioVenta()));
            } else {
                problemas.forEach(p -> errores.add(new ErrorFila(fila.numero(), p)));
            }
        }

        if (!filaPorCodigo.isEmpty()) {
            for (String existente : productoRepository.findCodigosExistentes(filaPorCodigo.keySet())) {
                errores.add(new ErrorFila(filaPorCodigo.get(existente),
                    "El código " + existente + " ya existe en el inventario (para cambiar su stock o precio, "
                        + "usa la ficha del producto)"));
            }
            errores.sort((a, b) -> Integer.compare(a.fila(), b.fila()));
        }

        if (filas.isEmpty()) {
            errores.add(new ErrorFila(0, "El archivo solo tiene la cabecera: no hay productos para importar"));
        }

        int creados = 0;
        if (confirmar && errores.isEmpty()) {
            for (ProductoCreateRequest req : solicitudes) {
                productoService.crear(req, MOTIVO_STOCK_IMPORTADO);
                creados++;
            }
        }
        // Las filas validas se devuelven aun con errores: la pantalla muestra que si se leyo bien
        validas.removeIf(v -> errores.stream().anyMatch(e -> e.fila() == v.fila()));
        return new ImportacionResultado(filas.size(), creados, validas, errores, ignoradas);
    }

    private record FilaLeida(int numero, List<String> celdas) {
        String celda(int indice) {
            return indice < celdas.size() ? celdas.get(indice).trim() : "";
        }
    }

    private static Map<Columna, Integer> leerCabecera(List<String> cabecera, List<ErrorFila> errores,
                                                      List<String> ignoradas) {
        Map<Columna, Integer> posiciones = new LinkedHashMap<>();
        for (int i = 0; i < cabecera.size(); i++) {
            String original = cabecera.get(i).trim();
            if (original.isEmpty()) continue;
            Columna columna = ALIAS_COLUMNAS.get(normalizar(original));
            if (columna == null) {
                ignoradas.add(original);
            } else if (posiciones.putIfAbsent(columna, i) != null) {
                errores.add(new ErrorFila(1, "La columna \"" + columna.nombreEnArchivo() + "\" está repetida"));
            }
        }
        for (Columna c : Columna.values()) {
            if (c.obligatoria && !posiciones.containsKey(c)) {
                errores.add(new ErrorFila(1, "Falta la columna obligatoria \"" + c.nombreEnArchivo() + "\""));
            }
        }
        return posiciones;
    }

    /** Devuelve la solicitud de alta, o null si la fila tiene algun problema (anotado en {@code problemas}). */
    private static ProductoCreateRequest convertir(FilaLeida fila, Map<Columna, Integer> posiciones,
                                                   List<String> problemas) {
        Map<Columna, String> v = new HashMap<>();
        for (Map.Entry<Columna, Integer> e : posiciones.entrySet()) {
            String valor = fila.celda(e.getValue());
            if (!valor.isEmpty()) {
                if (e.getKey().largoMaximo > 0 && valor.length() > e.getKey().largoMaximo) {
                    problemas.add("\"" + e.getKey().nombreEnArchivo() + "\" tiene más de "
                        + e.getKey().largoMaximo + " caracteres");
                }
                v.put(e.getKey(), valor);
            }
        }

        String codigo = obligatorio(v, Columna.CODIGO, problemas);
        String nombre = obligatorio(v, Columna.NOMBRE, problemas);
        TipoProducto tipo = null;
        String tipoTexto = obligatorio(v, Columna.TIPO, problemas);
        if (tipoTexto != null) {
            tipo = ALIAS_TIPOS.get(normalizar(tipoTexto));
            if (tipo == null) {
                problemas.add("Tipo desconocido \"" + tipoTexto + "\". Usa uno de: " + Arrays.stream(TipoProducto.values())
                    .map(t -> t.name().toLowerCase(Locale.ROOT).replace('_', ' '))
                    .collect(java.util.stream.Collectors.joining(", ")));
            }
        }
        Integer stock = entero(v.get(Columna.STOCK), "stock", problemas).orElse(0);
        Integer stockMinimo = entero(v.get(Columna.STOCK_MINIMO), "stock_minimo", problemas).orElse(null);
        BigDecimal precioCompra = precio(obligatorio(v, Columna.PRECIO_COMPRA, problemas), "precio_compra", problemas);
        BigDecimal precioVenta = precio(obligatorio(v, Columna.PRECIO_VENTA, problemas), "precio_venta", problemas);
        BigDecimal precioDuplicado = precio(v.get(Columna.PRECIO_DUPLICADO), "precio_duplicado", problemas);

        if (!problemas.isEmpty()) {
            return null;
        }
        return new ProductoCreateRequest(codigo, nombre, tipo, v.get(Columna.MARCA), stock, stockMinimo,
            precioCompra, precioVenta, null, v.get(Columna.PERFIL), v.get(Columna.COLOR), v.get(Columna.DISENO),
            v.get(Columna.MODELO), v.get(Columna.TAMANO), v.get(Columna.MATERIAL), precioDuplicado);
    }

    private static String obligatorio(Map<Columna, String> valores, Columna columna, List<String> problemas) {
        String valor = valores.get(columna);
        if (valor == null && columna.obligatoria) {
            problemas.add("Falta \"" + columna.nombreEnArchivo() + "\"");
        }
        return valor;
    }

    private static Optional<Integer> entero(String texto, String campo, List<String> problemas) {
        if (texto == null) return Optional.empty();
        BigDecimal numero = numero(texto);
        if (numero == null) {
            problemas.add("\"" + campo + "\" no es un número: " + texto);
            return Optional.empty();
        }
        if (numero.signum() < 0) {
            problemas.add("\"" + campo + "\" no puede ser negativo");
            return Optional.empty();
        }
        try {
            return Optional.of(numero.intValueExact());
        } catch (ArithmeticException ex) {
            problemas.add("\"" + campo + "\" debe ser un número entero: " + texto);
            return Optional.empty();
        }
    }

    private static BigDecimal precio(String texto, String campo, List<String> problemas) {
        if (texto == null) return null;
        BigDecimal numero = numero(texto);
        if (numero == null) {
            problemas.add("\"" + campo + "\" no es un precio válido: " + texto);
            return null;
        }
        if (numero.signum() < 0) {
            problemas.add("\"" + campo + "\" no puede ser negativo");
            return null;
        }
        numero = numero.setScale(2, RoundingMode.HALF_UP);
        if (numero.compareTo(PRECIO_MAXIMO) > 0) {
            problemas.add("\"" + campo + "\" es demasiado grande: " + texto);
            return null;
        }
        return numero;
    }

    /**
     * Acepta "12.50", "12,50", "S/ 12.50", "S/. 12.50", "1,250.00" y "1.250,00".
     * Una sola coma se toma como decimal: Excel con configuracion regional que
     * usa coma decimal guarda "12,5".
     */
    static BigDecimal numero(String texto) {
        String s = texto.replaceAll("(?i)s/\\.?", "").replaceAll("[\\s\\u00A0]", "");
        int ultimaComa = s.lastIndexOf(',');
        int ultimoPunto = s.lastIndexOf('.');
        if (ultimaComa >= 0 && ultimoPunto >= 0) {
            if (ultimaComa > ultimoPunto) {
                s = s.replace(".", "").replace(',', '.');
            } else {
                s = s.replace(",", "");
            }
        } else if (ultimaComa >= 0) {
            s = s.indexOf(',') == ultimaComa ? s.replace(',', '.') : s.replace(",", "");
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** "Precio de Venta" → "precio_de_venta" → sin "de": "precio_venta". */
    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String base = sinTildes.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
        String sinDe = base.replaceAll("_de_", "_");
        return ALIAS_COLUMNAS.containsKey(base) || ALIAS_TIPOS.containsKey(base) ? base : sinDe;
    }

    private static boolean esVacia(List<String> celdas) {
        return celdas.stream().allMatch(c -> c.trim().isEmpty());
    }
}
