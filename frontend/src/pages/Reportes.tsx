import { useEffect, useState } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { reportesApi } from '../api/reportes'
import { extraerMensajeError } from '../api/axiosClient'
import { Card, StatCard } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { formatearSoles } from '../utils/format'
import { hoyISO, inicioMesISO } from '../utils/fecha'
import type { ReporteVentasResponse, ProductoMasVendidoResponse, ReporteInventarioResponse } from '../types/reportes'

const COLORES = ['#5980a6', '#2c455d', '#7d9e86', '#b0975f', '#bd8080']


export function Reportes() {
  const [desde, setDesde] = useState(() => inicioMesISO(hoyISO()))
  const [hasta, setHasta] = useState(hoyISO)
  const [ventas, setVentas] = useState<ReporteVentasResponse | null>(null)
  const [masVendidos, setMasVendidos] = useState<ProductoMasVendidoResponse[]>([])
  const [inventario, setInventario] = useState<ReporteInventarioResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  // Al abrir la pantalla se muestra el mes en curso, sin esperar a "Consultar"
  useEffect(() => {
    consultar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function consultar() {
    setError(null)
    setCargando(true)
    try {
      const [v, mv, inv] = await Promise.all([
        reportesApi.ventas(desde, hasta),
        reportesApi.productosMasVendidos(desde, hasta),
        reportesApi.inventario(),
      ])
      setVentas(v)
      setMasVendidos(mv)
      setInventario(inv)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="flex flex-col gap-5">
      <h1 className="font-heading text-2xl font-semibold">Reportes</h1>

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

      {error && <p className="text-sm text-danger">{error}</p>}

      {ventas && (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
            <StatCard label="Total vendido" value={formatearSoles(ventas.totalVentas)} />
            <StatCard label="Transacciones" value={ventas.cantidadTransacciones} />
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
              <Bar dataKey="cantidadVendida" fill="#5980a6" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
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
    </div>
  )
}
