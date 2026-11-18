import { useEffect, useState, type ChangeEvent } from 'react'
import { BarChart3, Download, Upload } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { reportesApi } from '../api/reportes'
import { analisisApi } from '../api/analisis'
import { useAuth } from '../context/useAuth'
import { extraerMensajeError } from '../api/axiosClient'
import { Card, StatCard } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { formatearFecha, formatearSoles } from '../utils/format'
import { hoyISO, inicioMesISO } from '../utils/fecha'
import type {
  ReporteVentasResponse, ProductoMasVendidoResponse, ReporteInventarioResponse,
  ReporteServiciosResponse, ReporteClientesResponse, GrupoServicios,
} from '../types/reportes'
import type { ResultadoVentasAnteriores, ResumenVentasAnteriores } from '../types/analisis'

// Okabe-Ito: colores que se distinguen entre si con los daltonismos comunes
const COLORES = ['#0072B2', '#E69F00', '#009E73', '#CC79A7', '#56B4E9', '#D55E00']


export function Reportes() {
  const [desde, setDesde] = useState(() => inicioMesISO(hoyISO()))
  const [hasta, setHasta] = useState(hoyISO)
  const [ventas, setVentas] = useState<ReporteVentasResponse | null>(null)
  const [masVendidos, setMasVendidos] = useState<ProductoMasVendidoResponse[]>([])
  const [inventario, setInventario] = useState<ReporteInventarioResponse | null>(null)
  const [servicios, setServicios] = useState<ReporteServiciosResponse | null>(null)
  const [clientes, setClientes] = useState<ReporteClientesResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)
  const { usuario } = useAuth()
  const esAdmin = usuario?.role === 'ROLE_ADMIN'

  // Al abrir la pantalla se muestra el mes en curso, sin esperar a "Consultar"
  useEffect(() => {
    consultar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function consultar() {
    setError(null)
    setCargando(true)
    try {
      const [v, mv, inv, serv, cli] = await Promise.all([
        reportesApi.ventas(desde, hasta),
        reportesApi.productosMasVendidos(desde, hasta),
        reportesApi.inventario(),
        reportesApi.servicios(desde, hasta),
        reportesApi.clientes(desde, hasta),
      ])
      setVentas(v)
      setMasVendidos(mv)
      setInventario(inv)
      setServicios(serv)
      setClientes(cli)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="flex flex-col gap-5">
      <h1 className="font-heading text-3xl font-bold">Reportes</h1>

      <Card className="flex flex-wrap items-end gap-3">
        <div>
          <label className="mb-1 block text-xs text-sub">Desde</label>
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)}
            className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" />
        </div>
        <div>
          <label className="mb-1 block text-xs text-sub">Hasta</label>
          <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)}
            className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" />
        </div>
        <Button onClick={consultar} disabled={cargando}>{cargando ? 'Consultando…' : 'Consultar'}</Button>
      </Card>

      {error && <p className="text-base font-semibold text-danger">{error}</p>}

      {ventas && (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
            <StatCard label="Ingresos totales" value={formatearSoles(ventas.ingresosTotales)} />
            <StatCard label="Ventas de mostrador" value={formatearSoles(ventas.totalVentas)} />
            <StatCard label="Transacciones" value={ventas.cantidadTransacciones} />
            <StatCard label="Taller (neto de devoluciones)" value={formatearSoles(ventas.ingresosTaller)} />
            <StatCard
              label={`Domicilio (${ventas.serviciosDomicilioRealizados} realizados)`}
              value={formatearSoles(ventas.ingresosDomicilio)}
            />
            {inventario && <StatCard label="Valor de inventario" value={formatearSoles(inventario.valorTotalInventario)} />}
          </div>

          {ventas.porMetodoPago.length > 0 && (
            <Card variant="tecnico">
              <span className="font-heading mb-3 block text-lg font-semibold">Ventas por método de pago</span>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie data={ventas.porMetodoPago} dataKey="total" nameKey="metodoPago" outerRadius={80} label>
                    {ventas.porMetodoPago.map((_, i) => <Cell key={i} fill={COLORES[i % COLORES.length]} />)}
                  </Pie>
                  <Tooltip formatter={(v: number) => formatearSoles(v)} />
                </PieChart>
              </ResponsiveContainer>
            </Card>
          )}
        </>
      )}

      {masVendidos.length > 0 && (
        <Card variant="tecnico">
          <span className="font-heading mb-3 block text-lg font-semibold">Productos más vendidos</span>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={masVendidos} layout="vertical" margin={{ left: 24 }}>
              <XAxis type="number" />
              <YAxis type="category" dataKey="nombre" width={140} tick={{ fontSize: 12 }} />
              <Tooltip />
              <Bar dataKey="cantidadVendida" fill="#0072B2" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>
      )}

      {servicios && (
        <Card>
          <span className="font-heading mb-3 block text-lg font-semibold">Servicios realizados</span>
          <div className="mb-4 grid grid-cols-2 gap-3 md:grid-cols-4">
            <StatCard label="Domicilios realizados" value={servicios.domicilio.realizados} />
            <StatCard label="Cobrado en domicilios" value={formatearSoles(servicios.domicilio.cobrado)} />
            <StatCard label="Órdenes entregadas" value={servicios.taller.entregadas} />
            <StatCard label="Órdenes abiertas hoy" value={servicios.taller.abiertas} />
          </div>
          <p className="mb-3 text-xs text-sub">
            Taller: {servicios.taller.recibidas} órdenes recibidas, {formatearSoles(servicios.taller.ingresosNetos)} cobrados
            (neto de devoluciones). Domicilio: {servicios.domicilio.cancelados} servicios cancelados.
          </p>
          <div className="grid gap-4 md:grid-cols-2">
            <TablaGrupos titulo="Domicilio por tipo de trabajo" grupos={servicios.domicilio.porTipo} />
            <TablaGrupos titulo="Domicilio por técnico" grupos={servicios.domicilio.porTecnico} />
          </div>
        </Card>
      )}

      {clientes && (
        <Card>
          <span className="font-heading mb-1 block text-lg font-semibold">Clientes que más dejaron</span>
          <p className="mb-3 text-xs text-sub">
            Suma compras, taller y domicilio. {clientes.clientesConMovimiento} clientes con movimiento;
            las ventas sin cliente ({formatearSoles(clientes.ventasSinCliente)}) no se pueden atribuir a nadie.
          </p>
          {clientes.ranking.length === 0 ? (
            <p className="text-sm text-sub">Sin movimientos de clientes en estas fechas.</p>
          ) : (
            <div className="max-h-80 overflow-x-auto overflow-y-auto">
              <table className="w-full text-sm">
                <thead className="text-left text-xs text-sub">
                  <tr>
                    <th className="pb-2">Cliente</th>
                    <th className="pb-2 text-right">Compras</th>
                    <th className="pb-2 text-right">Taller</th>
                    <th className="pb-2 text-right">Domicilio</th>
                    <th className="pb-2 text-right">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hair">
                  {clientes.ranking.map((c) => (
                    <tr key={c.clienteId}>
                      <td className="py-1.5">{c.nombre}</td>
                      <td className="py-1.5 text-right">{formatearSoles(c.ventas.total)}</td>
                      <td className="py-1.5 text-right">{formatearSoles(c.taller.total)}</td>
                      <td className="py-1.5 text-right">{formatearSoles(c.domicilio.total)}</td>
                      <td className="py-1.5 text-right font-medium">{formatearSoles(c.total)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}

      {inventario && (
        <Card>
          <span className="font-heading mb-3 block text-lg font-semibold">Inventario general</span>
          <p className="mb-3 text-sm text-sub">
            {inventario.totalProductosActivos} productos activos · {inventario.totalUnidades} unidades totales
          </p>
          <div className="max-h-72 overflow-y-auto">
            <table className="w-full text-sm">
              <thead className="text-left text-xs text-sub">
                <tr><th className="pb-2">Producto</th><th className="pb-2">Stock</th><th className="pb-2 text-right">Valor</th></tr>
              </thead>
              <tbody className="divide-y divide-hair">
                {inventario.items.map((it) => (
                  <tr key={it.codigo}>
                    <td className="py-1.5">{it.nombre}</td>
                    <td className="py-1.5">{it.cantidadStock}</td>
                    <td className="py-1.5 text-right">{formatearSoles(it.valorLinea)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Para el dueño: los datos completos del negocio, para armar tableros en Power BI */}
      {esAdmin && <DatosPowerBi desde={desde} hasta={hasta} />}
      {esAdmin && <VentasAnteriores />}
    </div>
  )
}

function TablaGrupos({ titulo, grupos }: { titulo: string; grupos: GrupoServicios[] }) {
  return (
    <div>
      <span className="mb-2 block text-sm font-medium">{titulo}</span>
      {grupos.length === 0 ? (
        <p className="text-sm text-sub">Ninguno en estas fechas.</p>
      ) : (
        <table className="w-full text-sm">
          <tbody className="divide-y divide-hair">
            {grupos.map((g) => (
              <tr key={g.nombre}>
                <td className="py-1.5">{g.nombre}</td>
                <td className="py-1.5 text-right text-sub">{g.cantidad}</td>
                <td className="py-1.5 text-right">{formatearSoles(g.total)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

function descargarBlob(blob: Blob, nombre: string) {
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre
  enlace.click()
  URL.revokeObjectURL(url)
}

/** Un Excel con una tabla por tema; siempre con el mismo nombre para reemplazarlo en Power BI. */
function DatosPowerBi({ desde, hasta }: { desde: string; hasta: string }) {
  const [descargando, setDescargando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function descargar() {
    setError(null)
    setDescargando(true)
    try {
      descargarBlob(await analisisApi.powerBi(desde, hasta), 'cerrajeria-datos.xlsx')
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setDescargando(false)
    }
  }

  return (
    <Card className="flex flex-col gap-3">
      <span className="flex items-center gap-2 text-lg font-semibold">
        <BarChart3 size={22} aria-hidden /> Datos para Power BI
      </span>
      <p className="text-sm text-sub">
        Un Excel con las fechas de arriba ({formatearFecha(desde)} al {formatearFecha(hasta)}): ventas (con las
        anteriores), cobros, taller, domicilio, productos, kárdex, compras y cajas, cada uno en su hoja.
      </p>
      <Button onClick={descargar} disabled={descargando} className="self-start">
        <Download size={20} aria-hidden /> {descargando ? 'Preparando…' : 'Descargar Excel'}
      </Button>
      <p className="text-xs text-sub">
        En Power BI: Obtener datos → Excel → marca las hojas. Para actualizar el tablero, descarga de nuevo,
        reemplaza el archivo (se llama siempre igual) y pulsa Actualizar.
      </p>
      {error && <p className="text-sm font-semibold text-danger">{error}</p>}
    </Card>
  )
}

const PLANTILLA_VENTAS = '\uFEFFfecha;producto;codigo;cantidad;precio;metodo_pago\r\n'
  + '15/03/2025;Llave de casa;;10;2.50;efectivo\r\n'
  + '15/03/2025;Candado Forte 40 mm;CAN-40;1;30;yape\r\n'

/** Ventas de antes del sistema para el historial: no tocan la caja ni el stock. */
function VentasAnteriores() {
  const [resumen, setResumen] = useState<ResumenVentasAnteriores | null>(null)
  const [archivo, setArchivo] = useState<File | null>(null)
  const [revision, setRevision] = useState<ResultadoVentasAnteriores | null>(null)
  const [trabajando, setTrabajando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  useEffect(() => {
    analisisApi.resumenVentasAnteriores().then(setResumen).catch(() => setResumen(null))
  }, [])

  async function elegirArchivo(e: ChangeEvent<HTMLInputElement>) {
    const elegido = e.target.files?.[0]
    e.target.value = '' // permite volver a elegir el mismo archivo tras corregirlo
    if (!elegido) return
    setArchivo(elegido)
    setRevision(null)
    setAviso(null)
    setError(null)
    setTrabajando(true)
    try {
      setRevision(await analisisApi.cargarVentasAnteriores(elegido, false))
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setTrabajando(false)
    }
  }

  async function confirmar() {
    if (!archivo) return
    setTrabajando(true)
    setError(null)
    try {
      const r = await analisisApi.cargarVentasAnteriores(archivo, true)
      if (r.errores.length > 0) {
        setRevision(r)
      } else {
        setArchivo(null)
        setRevision(null)
        setAviso(`Listo: se cargaron ${r.cargadas} ventas.`)
        setResumen(await analisisApi.resumenVentasAnteriores())
      }
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setTrabajando(false)
    }
  }

  async function borrar() {
    if (!window.confirm('¿Borrar todas las ventas anteriores cargadas? Las ventas del sistema no se tocan.')) return
    setTrabajando(true)
    setError(null)
    try {
      const { borradas } = await analisisApi.borrarVentasAnteriores()
      setAviso(`Se borraron ${borradas} ventas anteriores.`)
      setResumen(await analisisApi.resumenVentasAnteriores())
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setTrabajando(false)
    }
  }

  const lista = revision !== null && revision.errores.length === 0 && revision.filasLeidas > 0

  return (
    <Card className="flex flex-col gap-3">
      <span className="text-lg font-semibold">Ventas de antes del sistema</span>
      <p className="text-sm text-sub">
        Si tienes ventas anotadas en un cuaderno o en Excel, súbelas para que el historial de Power BI empiece
        antes. Quedan aparte: <b>no cambian la caja ni el stock</b>.
      </p>
      {resumen && resumen.filas > 0 ? (
        <p className="text-sm">
          Cargadas: <b>{resumen.filas}</b> ventas por <b>{formatearSoles(resumen.total)}</b>, del{' '}
          {formatearFecha(resumen.desde!)} al {formatearFecha(resumen.hasta!)}.
        </p>
      ) : (
        <p className="text-sm">Todavía no hay ventas anteriores cargadas.</p>
      )}
      <ol className="list-decimal space-y-1 pl-5 text-sm text-sub">
        <li>
          <button type="button" className="text-steel underline"
            onClick={() => descargarBlob(new Blob([PLANTILLA_VENTAS], { type: 'text/csv' }), 'ventas-anteriores.csv')}>
            Descarga la plantilla
          </button>{' '}
          o usa tu Excel con las columnas <b>fecha</b>, <b>producto</b>, <b>cantidad</b> y <b>precio</b> (o <b>total</b>);
          <b> codigo</b> y <b>metodo_pago</b> son opcionales.
        </li>
        <li>Guárdalo como CSV y elígelo: primero ves lo que se entendió y nada se guarda hasta que confirmes.</li>
      </ol>
      <label className="inline-flex min-h-12 cursor-pointer items-center gap-2 self-start rounded-lg border-2 border-line bg-card px-4 font-semibold hover:bg-hair">
        <Upload size={20} aria-hidden /> {trabajando ? 'Revisando…' : 'Elegir archivo'}
        <input type="file" accept=".csv,text/csv" className="sr-only" onChange={elegirArchivo} disabled={trabajando} />
      </label>

      {revision && (
        <div className="flex flex-col gap-2 text-sm">
          <p>
            {revision.filasLeidas} ventas{revision.desde && <> del {formatearFecha(revision.desde)} al {formatearFecha(revision.hasta!)}</>}
            {' '}por <b>{formatearSoles(revision.total)}</b>. {revision.enlazadas} coinciden con productos del inventario.
          </p>
          {revision.columnasIgnoradas.length > 0 && (
            <p className="text-sub">Columnas que no se usan: {revision.columnasIgnoradas.join(', ')}.</p>
          )}
          {revision.errores.length > 0 && (
            <div className="rounded-lg bg-tint-peach p-3 text-warning">
              <p className="font-semibold">Corrige esto en el archivo y vuelve a elegirlo (no se cargó nada):</p>
              <ul className="list-disc pl-5">
                {revision.errores.slice(0, 20).map((e, i) => (
                  <li key={i}>{e.fila > 0 ? `Fila ${e.fila}: ` : ''}{e.mensaje}</li>
                ))}
              </ul>
              {revision.errores.length > 20 && <p>…y {revision.errores.length - 20} más.</p>}
            </div>
          )}
          {revision.muestra.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="text-left text-sub">
                  <tr><th className="py-1">Fecha</th><th>Producto</th><th className="text-right">Cant.</th><th className="text-right">Precio</th></tr>
                </thead>
                <tbody className="divide-y divide-hair">
                  {revision.muestra.map((l) => (
                    <tr key={l.fila}>
                      <td className="py-1">{formatearFecha(l.fecha)}</td>
                      <td>{l.producto}{l.enlazada && <span className="text-success"> ✓</span>}</td>
                      <td className="text-right">{l.cantidad}</td>
                      <td className="text-right">{formatearSoles(l.precioUnitario)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {revision.filasLeidas > revision.muestra.length && (
                <p className="text-xs text-sub">Se muestran las primeras {revision.muestra.length}.</p>
              )}
            </div>
          )}
          {lista && (
            <Button tamano="lg" onClick={confirmar} disabled={trabajando} className="self-start">
              Cargar {revision.filasLeidas} ventas
            </Button>
          )}
        </div>
      )}

      {aviso && <p className="text-sm font-semibold text-success">{aviso}</p>}
      {error && <p className="text-sm font-semibold text-danger">{error}</p>}
      {resumen && resumen.filas > 0 && (
        <button type="button" onClick={borrar} disabled={trabajando}
          className="self-start text-sm font-semibold text-danger underline">
          Borrar las ventas anteriores (para subirlas de nuevo)
        </button>
      )}
    </Card>
  )
}
