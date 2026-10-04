import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { axiosClient, extraerMensajeError } from '../api/axiosClient'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { useCaja } from '../context/useCaja'
import { useToast } from '../context/useToast'
import { formatearSoles, formatearFechaHora } from '../utils/format'
import type { AperturaCajaRequest, CierreCajaRequest, CierreCajaResponse } from '../types/caja'

export function Caja() {
  const navigate = useNavigate()
  const { caja, cajaAbierta, refrescarCaja } = useCaja()
  const { notificar } = useToast()
  const [montoApertura, setMontoApertura] = useState('')
  const [montoContado, setMontoContado] = useState('')
  const [resultadoCierre, setResultadoCierre] = useState<CierreCajaResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  async function handleAbrir(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setCargando(true)
    try {
      const body: AperturaCajaRequest = { montoApertura: Number(montoApertura) }
      await axiosClient.post('/caja/abrir', body)
      await refrescarCaja()
      notificar('Caja abierta')
      setMontoApertura('')
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setCargando(false)
    }
  }

  async function handleCerrar(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setCargando(true)
    try {
      const body: CierreCajaRequest = { montoContado: Number(montoContado) }
      const res = await axiosClient.post<CierreCajaResponse>('/caja/cerrar', body)
      setResultadoCierre(res.data)
      await refrescarCaja()
      notificar('Caja cerrada')
      setMontoContado('')
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setCargando(false)
    }
  }

  if (!cajaAbierta) {
    return (
      <div className="mx-auto flex max-w-sm flex-col gap-4">
        <h1 className="font-heading text-2xl font-semibold">Abrir caja</h1>
        <form onSubmit={handleAbrir} className="panel-cepillado flex flex-col gap-4 rounded-lg p-5">
          <div>
            <label className="mb-1 block text-sm font-medium">Monto inicial (S/)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              required
              value={montoApertura}
              onChange={(e) => setMontoApertura(e.target.value)}
              className="w-full rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel"
            />
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" disabled={cargando} className="w-full">
            {cargando ? 'Abriendo…' : 'Abrir caja'}
          </Button>
        </form>
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-sm flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-2xl font-semibold">Caja</h1>
        <Button variant="ghost" onClick={() => navigate('/caja/historial')}>Ver historial</Button>
      </div>

      <Card className="flex flex-col gap-2 text-sm">
        <div className="flex justify-between"><span className="text-sub">Abierta desde</span><span>{caja && formatearFechaHora(caja.fechaApertura)}</span></div>
        <div className="flex justify-between"><span className="text-sub">Monto inicial</span><span>{formatearSoles(caja?.montoApertura)}</span></div>
        <div className="flex justify-between"><span className="text-sub">Ingresos</span><span>{formatearSoles(caja?.montoIngresos)}</span></div>
        <div className="flex justify-between"><span className="text-sub">Egresos</span><span>{formatearSoles(caja?.montoEgresos)}</span></div>
        <div className="flex justify-between"><span className="text-sub">Menos cobros por Yape</span><span>{formatearSoles(caja?.montoYape)}</span></div>
        <div className="flex justify-between"><span className="text-sub">Menos cobros por Plin</span><span>{formatearSoles(caja?.montoPlin)}</span></div>
        {!!caja?.montoTarjeta && (
          <div className="flex justify-between"><span className="text-sub">Menos cobros con tarjeta</span><span>{formatearSoles(caja.montoTarjeta)}</span></div>
        )}
        <div className="flex justify-between border-t border-hair pt-2 font-semibold"><span>Efectivo esperado en caja</span><span>{formatearSoles(caja?.efectivoEsperado)}</span></div>
        <p className="text-xs text-sub">Yape y Plin no están en el cajón: el cierre se cuadra solo contra el efectivo.</p>
      </Card>

      {resultadoCierre ? (
        <Card className={resultadoCierre.diferencia === 0 ? 'bg-tint-mint' : 'bg-tint-peach'}>
          <p className="font-heading text-lg font-semibold">Caja cerrada</p>
          <p className="text-sm">Contado: {formatearSoles(resultadoCierre.montoContado)}</p>
          <p className="text-sm">
            Diferencia: {formatearSoles(resultadoCierre.diferencia)}{' '}
            {resultadoCierre.diferencia === 0 ? '(cuadrada)' : resultadoCierre.diferencia > 0 ? '(sobrante)' : '(faltante)'}
          </p>
        </Card>
      ) : (
        <form onSubmit={handleCerrar} className="panel-cepillado flex flex-col gap-4 rounded-lg p-5">
          <div>
            <label className="mb-1 block text-sm font-medium">Efectivo contado (S/)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              required
              value={montoContado}
              onChange={(e) => setMontoContado(e.target.value)}
              className="w-full rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel"
            />
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" variant="danger" disabled={cargando} className="w-full">
            {cargando ? 'Cerrando…' : 'Cerrar caja'}
          </Button>
        </form>
      )}
    </div>
  )
}
