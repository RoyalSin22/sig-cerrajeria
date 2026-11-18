package com.cerrajeria.sig.analisis;

import com.cerrajeria.sig.analisis.ExportacionPowerBiService.Col;
import com.cerrajeria.sig.analisis.ExportacionPowerBiService.Tipo;
import com.cerrajeria.sig.common.excel.EstiloExcel;
import com.cerrajeria.sig.common.excel.LibroXlsx;
import lombok.RequiredArgsConstructor;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Cuatro reportes en Excel para trabajar encima: cada uno es un libro con una hoja
 * "Resumen" (los numeros de siempre, con formulas donde se derivan unos de otros) y
 * tablas con filtros cuyos totales se recalculan al filtrar (SUBTOTAL). Estan hechos
 * para imprimirse en A4 sin tocar nada.
 *
 * Todo es SQL de solo lectura. Las ventas anteriores (cargadas desde archivo) cuentan
 * en las ventas, pero no en cobros ni en caja: nunca pasaron por la caja del sistema.
 */
@Service
@RequiredArgsConstructor
public class ReportesExcelService {

    public enum Reporte {
        RESUMEN("resumen-del-periodo"), CAJA("cierre-de-caja"), COMPRAS("compras-y-lista-de-compras"),
        TALLER("taller-y-domicilio");

        final String archivo;

        Reporte(String archivo) {
            this.archivo = archivo;
        }

        public String archivo() {
            return archivo;
        }

        public static Reporte de(String texto) {
            for (Reporte r : values()) {
                if (r.archivo.equals(texto) || r.name().equalsIgnoreCase(texto)) return r;
            }
            throw new IllegalArgumentException("Reporte desconocido: " + texto
                + " (usa resumen, caja, compras o taller)");
        }
    }

    /** Fila de la tabla donde empieza la cabecera: arriba van el titulo, el aviso y los totales. */
    private static final int CABECERA = 4;
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT);

    /** Lineas vendidas del periodo: las del sistema (no anuladas) y las anteriores. 4 parametros. */
    private static final String LINEAS = """
        SELECT CAST(v.fecha_hora AS DATE) AS fecha, v.id AS venta, d.cantidad AS unidades, d.subtotal AS monto,
               d.cantidad * COALESCE(p.precio_compra, 0) AS costo, p.codigo AS codigo,
               COALESCE(p.nombre, d.descripcion) AS producto, p.tipo_producto AS tipo
        FROM detalle_ventas d
        JOIN ventas v ON v.id = d.venta_id
        LEFT JOIN productos p ON p.id = d.producto_id
        WHERE v.anulada = FALSE AND v.fecha_hora >= ? AND v.fecha_hora < ?
        UNION ALL
        SELECT a.fecha, NULL, a.cantidad, a.cantidad * a.precio_unitario,
               a.cantidad * COALESCE(p.precio_compra, 0), p.codigo, COALESCE(p.nombre, a.descripcion), p.tipo_producto
        FROM ventas_anteriores a
        LEFT JOIN productos p ON p.id = a.producto_id
        WHERE a.fecha BETWEEN ? AND ?
        """;

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public byte[] generar(Reporte reporte, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        Periodo p = new Periodo(desde, hasta);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            Workbook libro = new Workbook(salida, "SIG Cerrajeria", "1.0");
            switch (reporte) {
                case RESUMEN -> resumenDelPeriodo(libro, p);
                case CAJA -> cierreDeCaja(libro, p);
                case COMPRAS -> comprasYLista(libro, p);
                case TALLER -> tallerYDomicilio(libro, p);
            }
            libro.finish();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo armar el reporte", ex);
        }
        return LibroXlsx.normalizar(salida.toByteArray());
    }

    /** Las fechas en los tipos que pide cada columna de la base. */
    private record Periodo(LocalDate desde, LocalDate hasta) {
        Timestamp inicio() {
            return Timestamp.valueOf(desde.atStartOfDay());
        }

        Timestamp fin() {
            return Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()); // excluido
        }

        Date d1() {
            return Date.valueOf(desde);
        }

        Date d2() {
            return Date.valueOf(hasta);
        }

        String texto() {
            return "Del " + DIA.format(desde) + " al " + DIA.format(hasta);
        }
    }

    // ================================================================== 1. Resumen del periodo

    private void resumenDelPeriodo(Workbook libro, Periodo p) {
        Worksheet h = libro.newWorksheet("Resumen");
        encabezado(h, "Resumen del periodo", p);
        h.width(0, 40);
        h.width(1, 18);
        h.width(2, 60);

        // Ventas, costo y tickets de una sola pasada sobre las lineas vendidas
        BigDecimal[] v = jdbc.query("SELECT COALESCE(SUM(CASE WHEN venta IS NOT NULL THEN monto END), 0), "
                + "COALESCE(SUM(CASE WHEN venta IS NULL THEN monto END), 0), COALESCE(SUM(costo), 0), "
                + "COUNT(DISTINCT venta), COALESCE(SUM(unidades), 0) FROM (" + LINEAS + ") t",
            rs -> {
                rs.next();
                return new BigDecimal[] {rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3),
                    rs.getBigDecimal(4), rs.getBigDecimal(5)};
            }, p.inicio(), p.fin(), p.d1(), p.d2());
        BigDecimal[] anuladas = jdbc.query("SELECT COUNT(*), COALESCE(SUM(monto_total), 0) FROM ventas "
                + "WHERE anulada = TRUE AND fecha_hora >= ? AND fecha_hora < ?",
            rs -> {
                rs.next();
                return new BigDecimal[] {rs.getBigDecimal(1), rs.getBigDecimal(2)};
            }, p.inicio(), p.fin());
        BigDecimal taller = suma("SELECT SUM(CASE WHEN tipo_pago = 'DEVOLUCION' THEN -monto ELSE monto END) "
            + "FROM pagos_orden WHERE fecha_hora >= ? AND fecha_hora < ?", p.inicio(), p.fin());
        BigDecimal domicilio = suma("SELECT SUM(monto_cobrado) FROM servicios_domicilio WHERE estado = 'REALIZADO' "
            + "AND fecha_realizado >= ? AND fecha_realizado < ?", p.inicio(), p.fin());
        BigDecimal compras = suma("SELECT SUM(precio_total) FROM compras WHERE fecha BETWEEN ? AND ?", p.d1(), p.d2());

        int f = 3;
        EstiloExcel.seccion(h, f++, "Ventas");
        int filaVentasSistema = kpi(h, f++, "Ventas del sistema", v[0], EstiloExcel.SOLES, "Lo vendido con la app, sin las anuladas.");
        int filaAnteriores = kpi(h, f++, "Ventas anteriores", v[1], EstiloExcel.SOLES, "Cargadas desde archivo (antes del sistema).");
        int filaTotal = kpiFormula(h, f++, "Ventas totales", "B" + filaVentasSistema + "+B" + filaAnteriores, EstiloExcel.SOLES, "");
        int filaCosto = kpi(h, f++, "Costo de lo vendido (aprox.)", v[2], EstiloExcel.SOLES,
            "Con el precio de compra de hoy; un servicio no tiene costo.");
        int filaGanancia = kpiFormula(h, f++, "Ganancia aprox.", "B" + filaTotal + "-B" + filaCosto, EstiloExcel.SOLES, "");
        kpiFormula(h, f++, "Margen", "IF(B" + filaTotal + "=0,0,B" + filaGanancia + "/B" + filaTotal + ")", "0%", "");
        int filaTickets = kpi(h, f++, "Ventas realizadas (tickets)", v[3], EstiloExcel.ENTERO, "Solo las del sistema: las anteriores no se cuentan por ticket.");
        kpiFormula(h, f++, "Ticket promedio", "IF(B" + filaTickets + "=0,0,B" + filaVentasSistema + "/B" + filaTickets + ")",
            EstiloExcel.SOLES, "");
        kpi(h, f++, "Unidades vendidas", v[4], EstiloExcel.ENTERO, "");
        kpi(h, f++, "Ventas anuladas", anuladas[0], EstiloExcel.ENTERO, "S/ " + anuladas[1].setScale(2, java.math.RoundingMode.HALF_UP) + " no cuentan arriba.");
        f++;
        EstiloExcel.seccion(h, f++, "Otros ingresos y gastos");
        kpi(h, f++, "Cobrado en el taller (neto)", taller, EstiloExcel.SOLES, "Adelantos y liquidaciones, menos devoluciones.");
        kpi(h, f++, "Cobrado en domicilios", domicilio, EstiloExcel.SOLES, "");
        kpi(h, f++, "Comprado a distribuidores", compras, EstiloExcel.SOLES, "");
        f++;
        h.value(f, 0, "Las demás hojas tienen el detalle, con filtros: Por día, Por método de pago, Productos, Por tipo y Clientes.");
        h.style(f, 0).italic().fontColor(EstiloExcel.SUAVE).set();
        EstiloExcel.imprimible(h, false, "Resumen del periodo");

        tabla(libro.newWorksheet("Por día"), "Ventas por día", p, "PorDia", List.of(
                col("Fecha", Tipo.FECHA, "fecha"), col("Tickets (sistema)", Tipo.ENTERO, "tickets"),
                col("Unidades", Tipo.ENTERO, "unidades"), col("Ventas", Tipo.DINERO, "ventas"),
                col("Costo aprox.", Tipo.DINERO, "costo"), col("Ganancia aprox.", Tipo.DINERO, "ganancia")),
            Set.of("tickets", "unidades", "ventas", "costo", "ganancia"), null, false,
            "SELECT fecha, COUNT(DISTINCT venta) AS tickets, SUM(unidades) AS unidades, SUM(monto) AS ventas, "
                + "SUM(costo) AS costo, SUM(monto - costo) AS ganancia FROM (" + LINEAS + ") t GROUP BY fecha ORDER BY fecha",
            p.inicio(), p.fin(), p.d1(), p.d2());

        tabla(libro.newWorksheet("Por método de pago"), "Cobrado por método de pago", p, "PorMetodo", List.of(
                col("Método de pago", Tipo.ETIQUETA, "metodo"), col("Ventas", Tipo.DINERO, "ventas"),
                col("Taller", Tipo.DINERO, "taller"), col("Domicilio", Tipo.DINERO, "domicilio"),
                col("Total", Tipo.DINERO, "total")),
            Set.of("ventas", "taller", "domicilio", "total"), null, false,
            """
            SELECT metodo, SUM(CASE WHEN origen = 'Venta' THEN monto ELSE 0 END) AS ventas,
                   SUM(CASE WHEN origen = 'Taller' THEN monto ELSE 0 END) AS taller,
                   SUM(CASE WHEN origen = 'Domicilio' THEN monto ELSE 0 END) AS domicilio, SUM(monto) AS total
            FROM (
              SELECT 'Venta' AS origen, COALESCE(pv.metodo_pago, v.metodo_pago) AS metodo,
                     COALESCE(pv.monto, v.monto_total) AS monto
              FROM ventas v
              LEFT JOIN pagos_venta pv ON pv.venta_id = v.id AND v.metodo_pago = 'MIXTO'
              WHERE v.anulada = FALSE AND v.fecha_hora >= ? AND v.fecha_hora < ?
              UNION ALL
              SELECT 'Taller', po.metodo_pago, CASE WHEN po.tipo_pago = 'DEVOLUCION' THEN -po.monto ELSE po.monto END
              FROM pagos_orden po WHERE po.fecha_hora >= ? AND po.fecha_hora < ?
              UNION ALL
              SELECT 'Domicilio', s.metodo_pago, s.monto_cobrado FROM servicios_domicilio s
              WHERE s.estado = 'REALIZADO' AND s.monto_cobrado > 0 AND s.fecha_realizado >= ? AND s.fecha_realizado < ?
            ) c GROUP BY metodo ORDER BY total DESC
            """, p.inicio(), p.fin(), p.inicio(), p.fin(), p.inicio(), p.fin());

        tabla(libro.newWorksheet("Productos"), "Productos más vendidos", p, "ProductosVendidos", List.of(
                col("Producto", Tipo.TEXTO, "producto"), col("Código", Tipo.TEXTO, "codigo"),
                col("Tipo", Tipo.ETIQUETA, "tipo"), col("Unidades", Tipo.ENTERO, "unidades"),
                col("Ventas", Tipo.DINERO, "ventas"), col("Ganancia aprox.", Tipo.DINERO, "ganancia")),
            Set.of("unidades", "ventas", "ganancia"), null, false,
            "SELECT producto, MAX(codigo) AS codigo, COALESCE(MAX(tipo), 'Servicio o sin producto') AS tipo, "
                + "SUM(unidades) AS unidades, SUM(monto) AS ventas, SUM(monto - costo) AS ganancia "
                + "FROM (" + LINEAS + ") t GROUP BY producto ORDER BY unidades DESC, ventas DESC",
            p.inicio(), p.fin(), p.d1(), p.d2());

        tabla(libro.newWorksheet("Por tipo"), "Ventas por tipo de producto", p, "PorTipo", List.of(
                col("Tipo", Tipo.ETIQUETA, "tipo"), col("Unidades", Tipo.ENTERO, "unidades"),
                col("Ventas", Tipo.DINERO, "ventas"), col("Ganancia aprox.", Tipo.DINERO, "ganancia")),
            Set.of("unidades", "ventas", "ganancia"), null, false,
            "SELECT COALESCE(tipo, 'Servicio o sin producto') AS tipo, SUM(unidades) AS unidades, SUM(monto) AS ventas, "
                + "SUM(monto - costo) AS ganancia FROM (" + LINEAS + ") t GROUP BY 1 ORDER BY ventas DESC",
            p.inicio(), p.fin(), p.d1(), p.d2());

        tabla(libro.newWorksheet("Clientes"), "Clientes que más dejaron", p, "Clientes", List.of(
                col("Cliente", Tipo.TEXTO, "cliente"), col("Compras", Tipo.DINERO, "ventas"),
                col("Taller", Tipo.DINERO, "taller"), col("Domicilio", Tipo.DINERO, "domicilio"),
                col("Total", Tipo.DINERO, "total")),
            Set.of("ventas", "taller", "domicilio", "total"), null, false,
            """
            SELECT cliente, SUM(ventas) AS ventas, SUM(taller) AS taller, SUM(domicilio) AS domicilio,
                   SUM(ventas + taller + domicilio) AS total
            FROM (
              SELECT c.nombre AS cliente, v.monto_total AS ventas, CAST(0 AS NUMERIC) AS taller, CAST(0 AS NUMERIC) AS domicilio
              FROM ventas v JOIN clientes c ON c.id = v.cliente_id
              WHERE v.anulada = FALSE AND v.fecha_hora >= ? AND v.fecha_hora < ?
              UNION ALL
              SELECT c.nombre, 0, CASE WHEN po.tipo_pago = 'DEVOLUCION' THEN -po.monto ELSE po.monto END, 0
              FROM pagos_orden po JOIN ordenes_trabajo o ON o.id = po.orden_trabajo_id JOIN clientes c ON c.id = o.cliente_id
              WHERE po.fecha_hora >= ? AND po.fecha_hora < ?
              UNION ALL
              SELECT c.nombre, 0, 0, s.monto_cobrado
              FROM servicios_domicilio s JOIN clientes c ON c.id = s.cliente_id
              WHERE s.estado = 'REALIZADO' AND s.monto_cobrado > 0 AND s.fecha_realizado >= ? AND s.fecha_realizado < ?
            ) t GROUP BY cliente ORDER BY total DESC
            """, p.inicio(), p.fin(), p.inicio(), p.fin(), p.inicio(), p.fin());
    }

    // ================================================================== 2. Cierre de caja

    private void cierreDeCaja(Workbook libro, Periodo p) {
        Worksheet h = libro.newWorksheet("Resumen");
        encabezado(h, "Cierre de caja", p);
        h.width(0, 40);
        h.width(1, 18);
        h.width(2, 60);

        BigDecimal[] c = jdbc.query("""
                SELECT COUNT(*), COALESCE(SUM(monto_apertura), 0), COALESCE(SUM(monto_ingresos), 0),
                       COALESCE(SUM(monto_egresos), 0), COALESCE(SUM(monto_yape), 0), COALESCE(SUM(monto_plin), 0),
                       COALESCE(SUM(monto_tarjeta), 0), COALESCE(SUM(monto_cierre), 0),
                       COALESCE(SUM(diferencia_cierre), 0),
                       COUNT(*) FILTER (WHERE COALESCE(diferencia_cierre, 0) <> 0),
                       COUNT(*) FILTER (WHERE estado = 'ABIERTA')
                FROM cajas_diarias WHERE fecha_apertura >= ? AND fecha_apertura < ?
                """,
            rs -> {
                rs.next();
                BigDecimal[] fila = new BigDecimal[11];
                for (int i = 0; i < 11; i++) fila[i] = rs.getBigDecimal(i + 1);
                return fila;
            }, p.inicio(), p.fin());

        int f = 3;
        EstiloExcel.seccion(h, f++, "Cajas del periodo");
        kpi(h, f++, "Cajas abiertas en el periodo", c[0], EstiloExcel.ENTERO, "Una por usuario y día.");
        kpi(h, f++, "Todavía sin cerrar", c[10], EstiloExcel.ENTERO, "");
        f++;
        EstiloExcel.seccion(h, f++, "El efectivo del cajón");
        int filaInicial = kpi(h, f++, "Monto inicial (suma de aperturas)", c[1], EstiloExcel.SOLES, "");
        int filaIngresos = kpi(h, f++, "Ingresos", c[2], EstiloExcel.SOLES, "Todo lo cobrado, con cualquier método de pago.");
        int filaEgresos = kpi(h, f++, "Egresos", c[3], EstiloExcel.SOLES, "Compras pagadas de caja, devoluciones y gastos.");
        int filaYape = kpi(h, f++, "Cobrado por Yape", c[4], EstiloExcel.SOLES, "No está en el cajón: no se cuenta.");
        int filaPlin = kpi(h, f++, "Cobrado por Plin", c[5], EstiloExcel.SOLES, "No está en el cajón: no se cuenta.");
        int filaTarjeta = kpi(h, f++, "Cobrado con tarjeta", c[6], EstiloExcel.SOLES, "No está en el cajón: no se cuenta.");
        int filaEsperado = kpiFormula(h, f++, "Efectivo que debía haber",
            "B" + filaInicial + "+B" + filaIngresos + "-B" + filaEgresos + "-B" + filaYape + "-B" + filaPlin + "-B" + filaTarjeta,
            EstiloExcel.SOLES, "Inicial + ingresos − egresos − lo digital.");
        kpi(h, f++, "Efectivo contado al cerrar", c[7], EstiloExcel.SOLES, "Solo de las cajas ya cerradas.");
        kpi(h, f++, "Diferencia total (sobra + / falta −)", c[8], EstiloExcel.SOLES, "Suma de las diferencias de cada cierre.");
        kpi(h, f++, "Cierres con diferencia", c[9], EstiloExcel.ENTERO, "Están resaltados en la hoja «Cajas».");
        h.style(filaEsperado - 1, 1).bold().set();
        f++;
        h.value(f, 0, "«Efectivo que debía haber» cuenta también las cajas aún abiertas; el contado, solo las cerradas.");
        h.style(f, 0).italic().fontColor(EstiloExcel.SUAVE).set();
        EstiloExcel.imprimible(h, false, "Cierre de caja");

        tabla(libro.newWorksheet("Cajas"), "Cajas del periodo", p, "Cajas", List.of(
                col("Fecha", Tipo.FECHA, "fecha"), col("Usuario", Tipo.TEXTO, "usuario"),
                col("Apertura", Tipo.FECHA_HORA, "fecha_apertura"), col("Cierre", Tipo.FECHA_HORA, "fecha_cierre"),
                col("Estado", Tipo.ETIQUETA, "estado"), col("Monto inicial", Tipo.DINERO, "monto_apertura"),
                col("Ingresos", Tipo.DINERO, "monto_ingresos"), col("Egresos", Tipo.DINERO, "monto_egresos"),
                col("Yape", Tipo.DINERO, "monto_yape"), col("Plin", Tipo.DINERO, "monto_plin"),
                col("Tarjeta", Tipo.DINERO, "monto_tarjeta"), col("Efectivo esperado", Tipo.DINERO, "esperado"),
                col("Efectivo contado", Tipo.DINERO, "monto_cierre"), col("Diferencia", Tipo.DINERO, "diferencia_cierre"),
                col("Observación", Tipo.TEXTO, "observacion_cierre")),
            Set.of("monto_apertura", "monto_ingresos", "monto_egresos", "monto_yape", "monto_plin", "monto_tarjeta",
                "esperado", "monto_cierre", "diferencia_cierre"),
            conDiferencia(), "Naranja = el cierre no cuadró.", true,
            """
            SELECT CAST(cd.fecha_apertura AS DATE) AS fecha, u.nombre AS usuario, cd.fecha_apertura, cd.fecha_cierre,
                   cd.estado, cd.monto_apertura, cd.monto_ingresos, cd.monto_egresos, cd.monto_yape, cd.monto_plin,
                   cd.monto_tarjeta,
                   cd.monto_apertura + cd.monto_ingresos - cd.monto_egresos - cd.monto_yape - cd.monto_plin - cd.monto_tarjeta AS esperado,
                   cd.monto_cierre, cd.diferencia_cierre, cd.observacion_cierre
            FROM cajas_diarias cd JOIN usuarios u ON u.id = cd.usuario_id
            WHERE cd.fecha_apertura >= ? AND cd.fecha_apertura < ?
            ORDER BY cd.fecha_apertura
            """, p.inicio(), p.fin());

        tabla(libro.newWorksheet("Movimientos"), "Ingresos y egresos puestos a mano en la caja", p, "Movimientos", List.of(
                col("Fecha", Tipo.FECHA, "fecha"), col("Fecha y hora", Tipo.FECHA_HORA, "fecha_hora"),
                col("Tipo", Tipo.ETIQUETA, "tipo"), col("Monto", Tipo.DINERO, "monto"),
                col("Motivo", Tipo.TEXTO, "motivo"), col("Usuario", Tipo.TEXTO, "usuario")),
            Set.of("monto"), null, false,
            """
            SELECT CAST(m.fecha_hora AS DATE) AS fecha, m.fecha_hora, m.tipo,
                   CASE WHEN m.tipo = 'EGRESO' THEN -m.monto ELSE m.monto END AS monto, m.motivo, u.nombre AS usuario
            FROM movimientos_caja m JOIN usuarios u ON u.id = m.usuario_id
            WHERE m.fecha_hora >= ? AND m.fecha_hora < ?
            ORDER BY m.fecha_hora
            """, p.inicio(), p.fin());
    }

    /** Resalta el cierre que no cuadro (sobro o falto plata). */
    private static Predicate<ResultSet> conDiferencia() {
        return rs -> {
            try {
                BigDecimal d = rs.getBigDecimal("diferencia_cierre");
                return d != null && d.signum() != 0;
            } catch (SQLException ex) {
                throw new IllegalStateException(ex);
            }
        };
    }

    // ================================================================== 3. Compras y lista de compras

    private void comprasYLista(Workbook libro, Periodo p) {
        Worksheet h = libro.newWorksheet("Resumen");
        encabezado(h, "Compras y lista de compras", p);
        h.width(0, 40);
        h.width(1, 18);
        h.width(2, 60);
        BigDecimal[] c = jdbc.query("SELECT COUNT(*), COALESCE(SUM(precio_total), 0), "
                + "COALESCE(SUM(precio_total) FILTER (WHERE caja_diaria_id IS NOT NULL), 0) FROM compras WHERE fecha BETWEEN ? AND ?",
            rs -> {
                rs.next();
                return new BigDecimal[] {rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3)};
            }, p.d1(), p.d2());
        BigDecimal bajos = suma("SELECT COUNT(*) FROM productos WHERE activo = TRUE AND cantidad_stock <= stock_minimo");

        int f = 3;
        EstiloExcel.seccion(h, f++, "Lo comprado en el periodo");
        kpi(h, f++, "Compras registradas", c[0], EstiloExcel.ENTERO, "");
        int filaTotal = kpi(h, f++, "Total comprado", c[1], EstiloExcel.SOLES, "");
        int filaCaja = kpi(h, f++, "Pagado de la caja", c[2], EstiloExcel.SOLES, "");
        kpiFormula(h, f++, "Pagado de otro modo", "B" + filaTotal + "-B" + filaCaja, EstiloExcel.SOLES, "No salió de la caja del sistema.");
        f++;
        EstiloExcel.seccion(h, f++, "Lo que falta comprar (stock de hoy)");
        kpi(h, f++, "Productos en stock bajo", bajos, EstiloExcel.ENTERO, "Con el stock igual o menor al mínimo.");
        h.value(f, 0, "Costo estimado de la reposición");
        if (bajos.signum() > 0) {
            h.formula(f, 1, "SUM(ListaCompras[Costo estimado])");
        } else {
            h.value(f, 1, BigDecimal.ZERO); // sin productos en stock bajo no hay tabla a la que apuntar
        }
        h.style(f, 1).format(EstiloExcel.SOLES).bold().set();
        h.value(f, 2, "Se recalcula si cambias «A comprar» en la hoja «Lista de compras».");
        h.style(f, 2).fontColor(EstiloExcel.SUAVE).set();
        f += 2;
        h.value(f, 0, "La hoja «Lista de compras» es para llevar al distribuidor: está lista para imprimir.");
        h.style(f, 0).italic().fontColor(EstiloExcel.SUAVE).set();
        EstiloExcel.imprimible(h, false, "Compras");

        listaDeCompras(libro.newWorksheet("Lista de compras"), p);

        tabla(libro.newWorksheet("Compras"), "Compras a distribuidores", p, "Compras", List.of(
                col("Fecha", Tipo.FECHA, "fecha"), col("Distribuidor", Tipo.TEXTO, "proveedor"),
                col("Código", Tipo.TEXTO, "codigo"), col("Producto", Tipo.TEXTO, "nombre"),
                col("Tipo", Tipo.ETIQUETA, "tipo"), col("Cantidad", Tipo.ENTERO, "cantidad"),
                col("Precio unitario", Tipo.DINERO, "precio_unitario"), col("Total", Tipo.DINERO, "precio_total"),
                col("Pagado de caja", Tipo.SI_NO, "de_caja"), col("Registró", Tipo.TEXTO, "usuario")),
            Set.of("cantidad", "precio_total"), null, false,
            """
            SELECT co.fecha, pr.nombre AS proveedor, p.codigo, p.nombre, p.tipo_producto AS tipo, co.cantidad,
                   co.precio_unitario, co.precio_total, co.caja_diaria_id IS NOT NULL AS de_caja, u.nombre AS usuario
            FROM compras co
            JOIN proveedores pr ON pr.id = co.proveedor_id
            JOIN productos p ON p.id = co.producto_id
            JOIN usuarios u ON u.id = co.usuario_id
            WHERE co.fecha BETWEEN ? AND ?
            ORDER BY co.fecha, co.id
            """, p.d1(), p.d2());

        tabla(libro.newWorksheet("Por distribuidor"), "Compras por distribuidor", p, "PorDistribuidor", List.of(
                col("Distribuidor", Tipo.TEXTO, "proveedor"), col("Compras", Tipo.ENTERO, "compras"),
                col("Unidades", Tipo.ENTERO, "unidades"), col("Total", Tipo.DINERO, "total")),
            Set.of("compras", "unidades", "total"), null, false,
            "SELECT pr.nombre AS proveedor, COUNT(*) AS compras, SUM(co.cantidad) AS unidades, SUM(co.precio_total) AS total "
                + "FROM compras co JOIN proveedores pr ON pr.id = co.proveedor_id WHERE co.fecha BETWEEN ? AND ? "
                + "GROUP BY pr.nombre ORDER BY total DESC", p.d1(), p.d2());
    }

    /**
     * Stock bajo de hoy, para llevar al distribuidor. "Sugerido" (lo que falta para el doble del
     * minimo, como la lista de compras de la app) y "Costo estimado" son formulas; "A comprar"
     * empieza igual al sugerido y es donde se corrige.
     */
    private void listaDeCompras(Worksheet h, Periodo p) {
        String[] titulos = {"Código", "Producto", "Tipo", "Stock", "Mínimo", "Vendidas 30 días", "Sugerido",
            "A comprar", "Precio compra", "Costo estimado", "Comprado"};
        double[] anchos = {18, 34, 20, 10, 10, 14, 11, 12, 14, 15, 12};
        encabezado(h, "Lista de compras", "Stock de hoy · lo que está en el mínimo o por debajo");
        h.value(2, 0, "Escribe en «A comprar» (amarillo) lo que sí vas a llevar: el costo se recalcula. Marca ✓ en «Comprado» al conseguirlo.");
        h.style(2, 0).italic().fontColor(EstiloExcel.SUAVE).set();
        for (int c = 0; c < titulos.length; c++) {
            h.value(CABECERA, c, titulos[c]);
            h.width(c, anchos[c]);
        }
        EstiloExcel.cabecera(h, CABECERA, 0, titulos.length - 1, EstiloExcel.AZUL);
        EstiloExcel.cabecera(h, CABECERA, 7, 7, "B7791F");

        LocalDateTime desde30 = LocalDate.now().minusDays(30).atStartOfDay();
        int[] n = {0};
        jdbc.query("""
            SELECT p.codigo, p.nombre, p.tipo_producto AS tipo, p.cantidad_stock, p.stock_minimo, p.precio_compra,
                   COALESCE((SELECT SUM(d.cantidad) FROM detalle_ventas d JOIN ventas v ON v.id = d.venta_id
                             WHERE d.producto_id = p.id AND v.anulada = FALSE AND v.fecha_hora >= ?), 0)
                 + COALESCE((SELECT SUM(a.cantidad) FROM ventas_anteriores a
                             WHERE a.producto_id = p.id AND a.fecha >= ?), 0) AS vendidas
            FROM productos p
            WHERE p.activo = TRUE AND p.cantidad_stock <= p.stock_minimo
            ORDER BY p.tipo_producto, p.nombre
            """, (ResultSet rs) -> {
            int r = CABECERA + 1 + n[0]++;
            int x = r + 1; // numero de fila en Excel
            int stock = rs.getInt("cantidad_stock");
            int minimo = rs.getInt("stock_minimo");
            h.value(r, 0, rs.getString("codigo"));
            h.value(r, 1, rs.getString("nombre"));
            h.value(r, 2, ExportacionPowerBiService.etiqueta(rs.getString("tipo")));
            h.value(r, 3, stock);
            h.value(r, 4, minimo);
            h.value(r, 5, rs.getLong("vendidas"));
            h.formula(r, 6, "MAX(0,2*E" + x + "-D" + x + ")");
            h.value(r, 7, Math.max(0, 2 * minimo - stock));
            h.value(r, 8, rs.getBigDecimal("precio_compra"));
            h.formula(r, 9, "H" + x + "*I" + x);
            h.style(r, 8).format(EstiloExcel.SOLES).set();
            h.style(r, 9).format(EstiloExcel.SOLES).set();
        }, Timestamp.valueOf(desde30), Date.valueOf(desde30.toLocalDate()));

        int total = n[0];
        if (total == 0) {
            h.value(CABECERA + 1, 0, "No hay productos en stock bajo. 👍");
            h.style(CABECERA + 1, 0).italic().fontColor(EstiloExcel.SUAVE).set();
        } else {
            int ultima = CABECERA + total;
            h.range(CABECERA + 1, 7, ultima, 7).style().fillColor(EstiloExcel.AMARILLO).bold().set();
            h.range(CABECERA + 1, 10, ultima, 10).style().horizontalAlignment("center").set();
            h.range(CABECERA + 1, 3, ultima, 3).style().fillColor(EstiloExcel.NARANJA).bold().set();
            EstiloExcel.bordes(h, CABECERA, 0, ultima, titulos.length - 1);
            h.range(CABECERA, 0, ultima, titulos.length - 1).createTable(titulos)
                .setName("ListaCompras").setDisplayName("ListaCompras");
            h.value(CABECERA - 1, 0, "Total (cambia con el filtro)");
            h.formula(CABECERA - 1, 9, "SUBTOTAL(109,J" + (CABECERA + 2) + ":J" + (ultima + 1) + ")");
            h.formula(CABECERA - 1, 7, "SUBTOTAL(109,H" + (CABECERA + 2) + ":H" + (ultima + 1) + ")");
            h.range(CABECERA - 1, 0, CABECERA - 1, titulos.length - 1).style().bold().fillColor(EstiloExcel.CELESTE).set();
            h.style(CABECERA - 1, 9).format(EstiloExcel.SOLES).set();
        }
        h.freezePane(0, CABECERA + 1);
        EstiloExcel.imprimible(h, true, "Lista de compras");
        h.repeatRows(CABECERA);
    }

    // ================================================================== 4. Taller y domicilio

    private void tallerYDomicilio(Workbook libro, Periodo p) {
        Worksheet h = libro.newWorksheet("Resumen");
        encabezado(h, "Taller y domicilio", p);
        h.width(0, 40);
        h.width(1, 18);
        h.width(2, 60);

        BigDecimal recibidas = suma("SELECT COUNT(*) FROM ordenes_trabajo WHERE fecha_ingreso >= ? AND fecha_ingreso < ?", p.inicio(), p.fin());
        BigDecimal entregadas = suma("SELECT COUNT(*) FROM ordenes_trabajo WHERE estado = 'ENTREGADO' AND fecha_entrega >= ? AND fecha_entrega < ?", p.inicio(), p.fin());
        BigDecimal canceladas = suma("SELECT COUNT(*) FROM ordenes_trabajo WHERE estado = 'CANCELADO' AND fecha_ingreso >= ? AND fecha_ingreso < ?", p.inicio(), p.fin());
        BigDecimal garantias = suma("SELECT COUNT(*) FROM ordenes_trabajo WHERE es_garantia = TRUE AND fecha_ingreso >= ? AND fecha_ingreso < ?", p.inicio(), p.fin());
        BigDecimal abiertas = suma("SELECT COUNT(*) FROM ordenes_trabajo WHERE estado NOT IN ('ENTREGADO', 'CANCELADO')");
        BigDecimal saldo = suma("SELECT SUM(saldo_pendiente) FROM ordenes_trabajo WHERE estado NOT IN ('ENTREGADO', 'CANCELADO')");
        BigDecimal cobradoTaller = suma("SELECT SUM(CASE WHEN tipo_pago = 'DEVOLUCION' THEN -monto ELSE monto END) FROM pagos_orden "
            + "WHERE fecha_hora >= ? AND fecha_hora < ?", p.inicio(), p.fin());
        BigDecimal realizados = suma("SELECT COUNT(*) FROM servicios_domicilio WHERE estado = 'REALIZADO' AND fecha_realizado >= ? AND fecha_realizado < ?", p.inicio(), p.fin());
        BigDecimal cancelados = suma("SELECT COUNT(*) FROM servicios_domicilio WHERE estado = 'CANCELADO' AND fecha BETWEEN ? AND ?", p.d1(), p.d2());
        BigDecimal cobradoDomicilio = suma("SELECT SUM(monto_cobrado) FROM servicios_domicilio WHERE estado = 'REALIZADO' "
            + "AND fecha_realizado >= ? AND fecha_realizado < ?", p.inicio(), p.fin());

        int f = 3;
        EstiloExcel.seccion(h, f++, "Taller");
        kpi(h, f++, "Órdenes recibidas", recibidas, EstiloExcel.ENTERO, "");
        kpi(h, f++, "Órdenes entregadas", entregadas, EstiloExcel.ENTERO, "Entregadas en el periodo (aunque se recibieron antes).");
        kpi(h, f++, "Órdenes canceladas", canceladas, EstiloExcel.ENTERO, "");
        kpi(h, f++, "Garantías", garantias, EstiloExcel.ENTERO, "Trabajos repetidos sin cobro.");
        kpi(h, f++, "Cobrado en el taller (neto)", cobradoTaller, EstiloExcel.SOLES, "Adelantos y liquidaciones, menos devoluciones.");
        kpi(h, f++, "Órdenes abiertas hoy", abiertas, EstiloExcel.ENTERO, "Sin entregar ni cancelar, sin importar la fecha.");
        kpi(h, f++, "Saldo por cobrar hoy", saldo, EstiloExcel.SOLES, "De las órdenes abiertas.");
        f++;
        EstiloExcel.seccion(h, f++, "Domicilio");
        int filaRealizados = kpi(h, f++, "Servicios realizados", realizados, EstiloExcel.ENTERO, "");
        kpi(h, f++, "Servicios cancelados", cancelados, EstiloExcel.ENTERO, "");
        int filaCobrado = kpi(h, f++, "Cobrado en domicilios", cobradoDomicilio, EstiloExcel.SOLES, "");
        kpiFormula(h, f++, "Cobro promedio por servicio", "IF(B" + filaRealizados + "=0,0,B" + filaCobrado + "/B" + filaRealizados + ")",
            EstiloExcel.SOLES, "");
        f++;
        h.value(f, 0, "Las hojas «Taller», «Domicilio» y «Por técnico» tienen el detalle, con filtros.");
        h.style(f, 0).italic().fontColor(EstiloExcel.SUAVE).set();
        EstiloExcel.imprimible(h, false, "Taller y domicilio");

        tabla(libro.newWorksheet("Taller"), "Órdenes de trabajo recibidas en el periodo", p, "Taller", List.of(
                col("Orden", Tipo.TEXTO, "numero_orden"), col("Ingreso", Tipo.FECHA, "ingreso"),
                col("Entrega", Tipo.FECHA, "entrega"), col("Estado", Tipo.ETIQUETA, "estado"),
                col("Cliente", Tipo.TEXTO, "cliente"), col("Teléfono", Tipo.TEXTO, "telefono"),
                col("Trabajo", Tipo.TEXTO, "producto_recibido"), col("Problema", Tipo.TEXTO, "problema"),
                col("Precio total", Tipo.DINERO, "precio_total"), col("Pagado", Tipo.DINERO, "monto_pagado"),
                col("Saldo", Tipo.DINERO, "saldo_pendiente"), col("Garantía", Tipo.SI_NO, "es_garantia"),
                col("Días", Tipo.ENTERO, "dias"), col("Registró", Tipo.TEXTO, "usuario")),
            Set.of("precio_total", "monto_pagado", "saldo_pendiente"), conSaldoPorCobrar(),
            "Naranja = lista para recoger, con saldo por cobrar.", true,
            """
            SELECT o.numero_orden, CAST(o.fecha_ingreso AS DATE) AS ingreso, CAST(o.fecha_entrega AS DATE) AS entrega,
                   o.estado, c.nombre AS cliente, c.telefono, o.producto_recibido, o.problema, o.precio_total,
                   o.monto_pagado, o.saldo_pendiente, o.es_garantia,
                   CAST(o.fecha_entrega AS DATE) - CAST(o.fecha_ingreso AS DATE) AS dias, u.nombre AS usuario
            FROM ordenes_trabajo o
            JOIN clientes c ON c.id = o.cliente_id
            JOIN usuarios u ON u.id = o.usuario_id
            WHERE o.fecha_ingreso >= ? AND o.fecha_ingreso < ?
            ORDER BY o.fecha_ingreso
            """, p.inicio(), p.fin());

        tabla(libro.newWorksheet("Domicilio"), "Servicios a domicilio del periodo", p, "Domicilio", List.of(
                col("Fecha", Tipo.FECHA, "fecha"), col("Hora", Tipo.TEXTO, "hora"),
                col("Servicio", Tipo.TEXTO, "tipo_servicio"), col("Estado", Tipo.ETIQUETA, "estado"),
                col("Cliente", Tipo.TEXTO, "cliente"), col("Dirección", Tipo.TEXTO, "direccion"),
                col("Teléfono", Tipo.TEXTO, "telefono"), col("Cobrado", Tipo.DINERO, "monto_cobrado"),
                col("Método de pago", Tipo.ETIQUETA, "metodo_pago"), col("Técnico", Tipo.TEXTO, "tecnico")),
            Set.of("monto_cobrado"), null, true,
            """
            SELECT s.fecha, to_char(s.hora, 'HH24:MI') AS hora, s.tipo_servicio, s.estado, c.nombre AS cliente,
                   s.direccion, s.telefono, s.monto_cobrado, s.metodo_pago, u.nombre AS tecnico
            FROM servicios_domicilio s
            JOIN clientes c ON c.id = s.cliente_id
            LEFT JOIN usuarios u ON u.id = s.realizado_por
            WHERE s.fecha BETWEEN ? AND ?
            ORDER BY s.fecha, s.hora
            """, p.d1(), p.d2());

        tabla(libro.newWorksheet("Por técnico"), "Servicios a domicilio por técnico", p, "PorTecnico", List.of(
                col("Técnico", Tipo.TEXTO, "tecnico"), col("Servicios", Tipo.ENTERO, "servicios"),
                col("Cobrado", Tipo.DINERO, "cobrado")),
            Set.of("servicios", "cobrado"), null, false,
            "SELECT u.nombre AS tecnico, COUNT(*) AS servicios, COALESCE(SUM(s.monto_cobrado), 0) AS cobrado "
                + "FROM servicios_domicilio s JOIN usuarios u ON u.id = s.realizado_por "
                + "WHERE s.estado = 'REALIZADO' AND s.fecha_realizado >= ? AND s.fecha_realizado < ? "
                + "GROUP BY u.nombre ORDER BY cobrado DESC", p.inicio(), p.fin());
    }

    /** Resalta una orden ya lista (o en curso) que todavia tiene plata por cobrar. */
    private static Predicate<ResultSet> conSaldoPorCobrar() {
        return rs -> {
            try {
                String estado = rs.getString("estado");
                return "LISTO".equals(estado) && rs.getBigDecimal("saldo_pendiente").signum() > 0;
            } catch (SQLException ex) {
                throw new IllegalStateException(ex);
            }
        };
    }

    // ================================================================== piezas comunes

    private static Col col(String titulo, Tipo tipo, String columna) {
        return new Col(titulo, tipo, columna);
    }

    private BigDecimal suma(String sql, Object... parametros) {
        BigDecimal valor = jdbc.queryForObject(sql, BigDecimal.class, parametros);
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private String nombreNegocio() {
        List<String> nombre = jdbc.queryForList("SELECT nombre FROM negocio WHERE id = 1", String.class);
        return nombre.isEmpty() ? "Cerrajería" : nombre.get(0);
    }

    private void encabezado(Worksheet h, String titulo, Periodo p) {
        encabezado(h, titulo, p.texto());
    }

    private void encabezado(Worksheet h, String titulo, String detalle) {
        h.hideGridLines();
        EstiloExcel.titulo(h, 0, nombreNegocio() + " · " + titulo);
        EstiloExcel.subtitulo(h, 1, detalle + " · Generado el "
            + DIA.format(LocalDate.now()) + " a las " + String.format("%tR", LocalDateTime.now()));
    }

    /** Una fila "etiqueta / valor / nota" de un resumen. Devuelve el numero de fila en Excel (para formulas). */
    private static int kpi(Worksheet h, int fila, String etiqueta, Object valor, String formato, String nota) {
        h.value(fila, 0, etiqueta);
        if (valor instanceof BigDecimal b) {
            h.value(fila, 1, b);
        }
        return kpiEstilo(h, fila, formato, nota);
    }

    private static int kpiFormula(Worksheet h, int fila, String etiqueta, String formula, String formato, String nota) {
        h.value(fila, 0, etiqueta);
        h.formula(fila, 1, formula);
        return kpiEstilo(h, fila, formato, nota);
    }

    private static int kpiEstilo(Worksheet h, int fila, String formato, String nota) {
        h.style(fila, 1).format(formato).bold().horizontalAlignment("right").set();
        if (!nota.isEmpty()) {
            h.value(fila, 2, "   " + nota); // un respiro entre el valor y su nota
            h.style(fila, 2).fontColor(EstiloExcel.SUAVE).set();
        }
        h.range(fila, 0, fila, 2).style().borderStyle(org.dhatim.fastexcel.BorderStyle.THIN).borderColor(EstiloExcel.LINEA).set();
        return fila + 1;
    }

    /**
     * Una hoja con titulo, aviso, totales que siguen al filtro (SUBTOTAL) y la tabla de datos.
     * {@code sumar} son los alias de las columnas que llevan total; {@code resaltar} marca filas
     * que piden atencion; {@code ancha} deja la hoja horizontal al imprimir.
     */
    private void tabla(Worksheet h, String titulo, Periodo p, String nombre, List<Col> cols, Set<String> sumar,
                       Predicate<ResultSet> resaltar, boolean ancha, String sql, Object... parametros) {
        tabla(h, titulo, p, nombre, cols, sumar, resaltar, null, ancha, sql, parametros);
    }

    /** {@code leyenda} explica por que se resalta una fila (solo si hay resaltado). */
    private void tabla(Worksheet h, String titulo, Periodo p, String nombre, List<Col> cols, Set<String> sumar,
                       Predicate<ResultSet> resaltar, String leyenda, boolean ancha, String sql, Object... parametros) {
        encabezado(h, titulo, p);
        h.value(2, 0, "Usa los filtros de la cabecera: el TOTAL de arriba se recalcula con lo que dejes a la vista."
            + (leyenda == null ? "" : " " + leyenda));
        h.style(2, 0).italic().fontColor(EstiloExcel.SUAVE).set();
        int n = cols.size();
        for (int c = 0; c < n; c++) {
            h.value(CABECERA, c, cols.get(c).titulo());
            h.width(c, ancho(cols.get(c)));
        }
        EstiloExcel.cabecera(h, CABECERA, 0, n - 1, EstiloExcel.AZUL);

        int[] filas = {0};
        jdbc.query(sql, (ResultSet rs) -> {
            int r = CABECERA + 1 + filas[0]++;
            if (resaltar != null && resaltar.test(rs)) {
                h.range(r, 0, r, n - 1).style().fillColor(EstiloExcel.NARANJA).set();
            }
            for (int c = 0; c < n; c++) {
                ExportacionPowerBiService.escribir(h, r, c, cols.get(c), rs);
            }
        }, parametros);

        int total = filas[0];
        if (total == 0) {
            h.value(CABECERA + 1, 0, "No hay datos en este periodo.");
            h.style(CABECERA + 1, 0).italic().fontColor(EstiloExcel.SUAVE).set();
        } else {
            int ultima = CABECERA + total;
            h.range(CABECERA, 0, ultima, n - 1).createTable(cols.stream().map(Col::titulo).toArray(String[]::new))
                .setName(nombre).setDisplayName(nombre);
            h.value(CABECERA - 1, 0, "TOTAL");
            h.range(CABECERA - 1, 0, CABECERA - 1, n - 1).style().bold().fillColor(EstiloExcel.CELESTE).set();
            for (int c = 0; c < n; c++) {
                Col col = cols.get(c);
                if (sumar.contains(col.columna())) {
                    String letra = letra(c);
                    h.formula(CABECERA - 1, c, "SUBTOTAL(109," + letra + (CABECERA + 2) + ":" + letra + (ultima + 1) + ")");
                    h.style(CABECERA - 1, c).format(col.tipo() == Tipo.DINERO ? EstiloExcel.SOLES : EstiloExcel.ENTERO).set();
                }
            }
        }
        h.freezePane(0, CABECERA + 1);
        EstiloExcel.imprimible(h, ancha || n > 6, titulo);
        h.repeatRows(CABECERA);
    }

    private static double ancho(Col col) {
        return switch (col.tipo()) {
            case FECHA -> 12;
            case FECHA_HORA -> 17;
            case HORA, ENTERO, SI_NO -> 12;
            case DINERO -> 15;
            default -> anchoTexto(col.titulo());
        };
    }

    /** Texto: lo que cada columna necesita para no cortarse en pantalla ni al imprimir. */
    private static double anchoTexto(String titulo) {
        return switch (titulo) {
            case "Producto", "Distribuidor" -> 32;
            case "Estado", "Tipo", "Cliente", "Trabajo", "Servicio", "Método de pago", "Técnico", "Teléfono" -> 22;
            case "Dirección", "Problema", "Motivo", "Observación" -> 30;
            case "Orden", "Código", "Usuario", "Registró" -> 16;
            default -> Math.max(16, Math.min(40, titulo.length() + 10));
        };
    }

    /** 0 → A, 25 → Z, 26 → AA. */
    private static String letra(int columna) {
        StringBuilder sb = new StringBuilder();
        for (int c = columna; c >= 0; c = c / 26 - 1) {
            sb.insert(0, (char) ('A' + c % 26));
        }
        return sb.toString();
    }
}
