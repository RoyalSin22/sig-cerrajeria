import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertTriangle, ShoppingCart, UserPlus, Wrench } from 'lucide-react'
import { axiosClient, extraerMensajeError } from '../api/axiosClient'
import { StatCard, Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { PullToRefreshIndicator } from '../components/ui/PullToRefreshIndicator'
import { usePullToRefresh } from '../hooks/usePullToRefresh'
import { useCaja } from '../context/CajaContext'
import { formatearSoles, formatearFechaHora } from '../utils/format'
import type { DashboardResponse } from '../types/dashboard'

export function Dashboard() {
  const navigate = useNavigate()
  const { cajaAbierta } = useCaja()
  const [data, setData] = useState<DashboardResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(async () => {
    try {
      const res = await axiosClient.get<DashboardResponse>('/dashboard')
      setData(res.data)
      setError(null)
    } catch (err) {
      setError(extraerMensajeError(err, 'No se pudo cargar el panel principal'))
    }
  }, [])

  const { refrescando, distancia, umbral } = usePullToRefresh(cargar)

  useEffect(() => {
    cargar()
  }, [cargar])

  if (error) {
    return <p className="text-danger">{error}</p>
  }

  if (!data) {
    return <p className="text-sub">Cargando panel…</p>
  }

  return (
    <div className="flex flex-col gap-5">
      <PullToRefreshIndicator distancia={distancia} refrescando={refrescando} umbral={umbral} />
      {!cajaAbierta && (
        <Card className="flex items-center justify-between bg-tint-peach">
          <span className="text-sm font-medium">No tienes una caja abierta. Ábrela para poder vender.</span>
          <Button variant="secondary" onClick={() => navigate('/caja')}>
            Abrir caja
          </Button>
        </Card>
      )}

      {/* Accesos rápidos (RF-10) */}
      <div className="flex gap-2 overflow-x-auto">
        <Button onClick={() => navigate('/venta')} className="flex items-center gap-2">
          <ShoppingCart size={16} /> Nueva venta
        </Button>
        <Button variant="secondary" onClick={() => navigate('/taller')} className="flex items-center gap-2">
          <Wrench size={16} /> Nueva orden
        </Button>
        <Button variant="secondary" onClick={() => navigate('/clientes')} className="flex items-center gap-2">
          <UserPlus size={16} /> Nuevo cliente
        </Button>
      </div>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        {/* Los montos ocupan el ancho completo en movil; los contadores van de a dos */}
        <StatCard label="Ventas de hoy" value={formatearSoles(data.ventasHoy)} tint="sky" className="col-span-2 sm:col-span-1" />
        <StatCard label="Ventas del mes" value={formatearSoles(data.ventasMes)} className="col-span-2 sm:col-span-1" />
        <StatCard label="Trabajos pendientes" value={data.trabajosPendientes} tint="mint" />
        <StatCard label="Trabajos listos" value={data.trabajosListos} />
        <StatCard label="Servicios próximos" value={data.serviciosDomicilioProximos} tint="peach" />
        <StatCard
          label="Stock bajo"
          value={data.productosStockBajo}
          tint="lilac"
          hint={data.productosStockBajo > 0 ? 'Requiere atención' : undefined}
        />
      </div>

      {data.alertasStockBajo.length > 0 && (
        <Card className="flex flex-col gap-2">
          <div className="flex items-center gap-2 text-warning">
            <AlertTriangle size={18} />
            <span className="font-heading text-lg font-semibold">Alertas de stock bajo</span>
          </div>
          <ul className="flex flex-col gap-1 text-sm">
            {data.alertasStockBajo.map((nombre) => (
              <li key={nombre} className="text-sub">• {nombre}</li>
            ))}
          </ul>
          <Button variant="ghost" onClick={() => navigate('/inventario')} className="self-start">
            Ver inventario completo
          </Button>
        </Card>
      )}

      <Card>
        <span className="font-heading mb-3 block text-lg font-semibold">Últimos movimientos</span>
        {data.ultimosMovimientos.length === 0 ? (
          <p className="text-sm text-sub">Todavía no hay actividad registrada.</p>
        ) : (
          <ul className="flex flex-col divide-y divide-hair">
            {data.ultimosMovimientos.map((m, i) => (
              <li key={i} className="flex items-center justify-between py-2 text-sm">
                <span>{m.descripcion}</span>
                <span className="text-xs text-sub">
                  {m.usuario} · {formatearFechaHora(m.fechaHora)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
