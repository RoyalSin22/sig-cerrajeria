import { useEffect, useState } from 'react'
import { ventasApi } from '../api/ventas'
import { extraerMensajeError } from '../api/axiosClient'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { useAuth } from '../context/useAuth'
import { useToast } from '../context/useToast'
import { formatearSoles, formatearFechaHora } from '../utils/format'
import { ETIQUETA_METODO_PAGO } from '../types/ventas'
import type { VentaResponse } from '../types/ventas'

export function HistorialVentas() {
  const { usuario } = useAuth()
  const { notificar } = useToast()
  const [ventas, setVentas] = useState<VentaResponse[]>([])
  const [pagina, setPagina] = useState(0)
  const [hayMas, setHayMas] = useState(false)
  const [cargando, setCargando] = useState(false)
  const [anulando, setAnulando] = useState<number | null>(null)

  async function cargar(numero = 0, acumular = false) {
    setCargando(true)
    try {
      const res = await ventasApi.listar(numero)
      setVentas((prev) => (acumular ? [...prev, ...res.content] : res.content))
      setPagina(res.page.number)
      setHayMas(res.page.number + 1 < res.page.totalPages)
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cargar(0, false)
  }, [])

  async function handleAnular(venta: VentaResponse) {
    if (!confirm(`¿Anular la venta del ${formatearFechaHora(venta.fechaHora)} por ${formatearSoles(venta.montoTotal)}? Se revertirá el stock.`)) {
      return
    }
    setAnulando(venta.id)
    try {
      const actualizada = await ventasApi.anular(venta.id)
      setVentas((prev) => prev.map((v) => (v.id === actualizada.id ? actualizada : v)))
      notificar('Venta anulada, stock revertido')
    } catch (err) {
      notificar(extraerMensajeError(err), 'error')
    } finally {
      setAnulando(null)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <h1 className="font-heading text-2xl font-semibold">Historial de ventas</h1>

      {cargando && ventas.length === 0 ? (
        <p className="text-sm text-sub">Cargando…</p>
      ) : ventas.length === 0 ? (
        <EmptyState mensaje="Todavía no hay ventas registradas." />
      ) : (
        <>
          <div className="flex flex-col gap-2">
            {ventas.map((v) => (
              <Card key={v.id} className={v.anulada ? 'opacity-60' : ''}>
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="text-sm font-medium">
                      {formatearFechaHora(v.fechaHora)} {v.cliente && `· ${v.cliente}`}
                    </p>
                    <p className="text-xs text-sub">
                      {ETIQUETA_METODO_PAGO[v.metodoPago]} · {v.detalles.length} producto(s)
                    </p>
                    {v.desglosePago.length > 0 && (
                      <p className="text-xs text-sub">
                        ({v.desglosePago.map((d) => `${ETIQUETA_METODO_PAGO[d.metodoPago]}: ${formatearSoles(d.monto)}`).join(' + ')})
                      </p>
                    )}
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    <span className="font-heading font-semibold text-steel-dark">{formatearSoles(v.montoTotal)}</span>
                    {v.anulada && <Badge tono="warning">Anulada</Badge>}
                  </div>
                </div>
                {!v.anulada && usuario?.role === 'ROLE_ADMIN' && (
                  <Button
                    variant="danger"
                    disabled={anulando === v.id}
                    onClick={() => handleAnular(v)}
                    className="mt-2 text-xs"
                  >
                    {anulando === v.id ? 'Anulando…' : 'Anular venta'}
                  </Button>
                )}
              </Card>
            ))}
          </div>
          {hayMas && (
            <Button variant="secondary" disabled={cargando} onClick={() => cargar(pagina + 1, true)} className="self-center">
              {cargando ? 'Cargando…' : 'Cargar más'}
            </Button>
          )}
        </>
      )}
    </div>
  )
}
