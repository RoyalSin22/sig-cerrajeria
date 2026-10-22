import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertTriangle, KeyRound, Package, ShoppingCart, Truck, Wallet, Wrench, type LucideIcon } from 'lucide-react'
import { axiosClient, extraerMensajeError } from '../api/axiosClient'
import { StatCard, Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { PullToRefreshIndicator } from '../components/ui/PullToRefreshIndicator'
import { usePullToRefresh } from '../hooks/usePullToRefresh'
import { useCaja } from '../context/useCaja'
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
    return <p className="text-base font-semibold text-danger">{error}</p>
  }

  if (!data) {
    return <p className="text-sub">Cargando panel…</p>
  }

  return (
    <div className="flex flex-col gap-5">
      <PullToRefreshIndicator distancia={distancia} refrescando={refrescando} umbral={umbral} />
      <h1 className="font-heading text-3xl font-bold">¿Qué vas a hacer?</h1>

      {!cajaAbierta && (
        <Card className="flex flex-col gap-3 border-2 border-warning bg-tint-peach sm:flex-row sm:items-center sm:justify-between">
          <span className="flex items-center gap-2 text-lg font-semibold">
            <Wallet size={26} className="shrink-0 text-warning" aria-hidden />
            La caja está cerrada. Ábrela para poder cobrar.
          </span>
          <Button tamano="lg" onClick={() => navigate('/caja')}>Abrir la caja</Button>
        </Card>
      )}

      {/* Accesos rapidos (RF-10): botones grandes, con icono y nombre, a un toque de cada tarea */}
      <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
        <Acceso icono={KeyRound} texto="Duplicado de llave" principal onClick={() => navigate('/venta?duplicado=1')} />
        <Acceso icono={ShoppingCart} texto="Vender" principal onClick={() => navigate('/venta')} />
        <Acceso icono={Wrench} texto="Recibir trabajo" onClick={() => navigate('/taller?nueva=1')} />
        <Acceso icono={Package} texto="Ver stock" onClick={() => navigate('/inventario')} />
        <Acceso icono={Truck} texto="Domicilio" onClick={() => navigate('/domicilio')} />
        <Acceso icono={Wallet} texto="Caja" onClick={() => navigate('/caja')} />
      </div>

      <h2 className="font-heading pt-2 text-2xl font-bold">Hoy</h2>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <StatCard label="Vendido hoy" value={formatearSoles(data.ventasHoy)} tint="sky" className="col-span-2 sm:col-span-1" />
        <StatCard label="Trabajos listos para entregar" value={data.trabajosListos} tint="mint" />
        <StatCard label="Trabajos en el taller" value={data.trabajosPendientes} />
        <StatCard label="Servicios a domicilio próximos" value={data.serviciosDomicilioProximos} tint="peach" />
        <StatCard
          label="Productos por acabarse"
          value={data.productosStockBajo}
          tint="lilac"
          hint={data.productosStockBajo > 0 ? 'Revisa la lista de compras' : undefined}
        />
        <StatCard label="Vendido este mes" value={formatearSoles(data.ventasMes)} className="col-span-2 sm:col-span-1" />
      </div>

      {data.alertasStockBajo.length > 0 && (
        <Card className="flex flex-col gap-2">
          <div className="flex items-center gap-2 text-warning">
            <AlertTriangle size={24} aria-hidden />
            <span className="font-heading text-xl font-bold">Se están acabando</span>
          </div>
          <ul className="flex flex-col gap-1 text-base">
            {data.alertasStockBajo.map((nombre) => (
              <li key={nombre}>• {nombre}</li>
            ))}
          </ul>
          <Button variant="secondary" onClick={() => navigate('/compras/lista')} className="self-start">
            Armar la lista de compras
          </Button>
        </Card>
      )}

      <Card>
        <span className="font-heading mb-3 block text-xl font-bold">Últimos movimientos</span>
        {data.ultimosMovimientos.length === 0 ? (
          <p className="text-base text-sub">Todavía no hay actividad registrada.</p>
        ) : (
          <ul className="flex flex-col divide-y divide-hair">
            {data.ultimosMovimientos.map((m, i) => (
              <li key={i} className="flex flex-col py-2 sm:flex-row sm:items-center sm:justify-between">
                <span className="text-base">{m.descripcion}</span>
                <span className="text-sm text-sub">
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

/** Boton grande de la pantalla de inicio: icono arriba, nombre debajo. */
function Acceso({
  icono: Icono, texto, onClick, principal,
}: { icono: LucideIcon; texto: string; onClick: () => void; principal?: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex min-h-28 flex-col items-center justify-center gap-2 rounded-2xl border-2 p-3 text-center text-lg font-bold leading-tight transition-colors ${
        principal
          ? 'border-steel-dark bg-steel-dark text-white hover:opacity-90'
          : 'border-hair bg-card text-ink hover:border-steel'
      }`}
    >
      <Icono size={36} aria-hidden />
      {texto}
    </button>
  )
}
