import { useEffect, useState } from 'react'
import { cajaApi } from '../api/caja'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { formatearSoles, formatearFechaHora } from '../utils/format'
import type { CajaDiariaResponse } from '../types/caja'

/** RF-37: historial paginado de sesiones de caja del usuario en sesión. */
export function HistorialCaja() {
  const [sesiones, setSesiones] = useState<CajaDiariaResponse[]>([])
  const [pagina, setPagina] = useState(0)
  const [hayMas, setHayMas] = useState(false)
  const [cargando, setCargando] = useState(false)

  async function cargar(numero = 0, acumular = false) {
    setCargando(true)
    try {
      const res = await cajaApi.historial(numero)
      setSesiones((prev) => (acumular ? [...prev, ...res.content] : res.content))
      setPagina(res.page.number)
      setHayMas(res.page.number + 1 < res.page.totalPages)
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cargar(0, false)
  }, [])

  return (
    <div className="flex flex-col gap-4">
      <h1 className="font-heading text-2xl font-semibold">Historial de caja</h1>

      {cargando && sesiones.length === 0 ? (
        <p className="text-sm text-sub">Cargando…</p>
      ) : sesiones.length === 0 ? (
        <EmptyState mensaje="Todavía no tienes sesiones de caja registradas." />
      ) : (
        <>
          <div className="flex flex-col gap-2">
            {sesiones.map((s) => (
              <Card key={s.id} className="flex flex-col gap-1">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">{formatearFechaHora(s.fechaApertura)}</span>
                  <Badge tono={s.estado === 'ABIERTA' ? 'success' : 'neutral'}>
                    {s.estado === 'ABIERTA' ? 'Abierta' : 'Cerrada'}
                  </Badge>
                </div>
                <div className="grid grid-cols-2 gap-1 text-xs text-sub">
                  <span>Apertura: {formatearSoles(s.montoApertura)}</span>
                  <span>Cierre: {s.montoCierre !== null ? formatearSoles(s.montoCierre) : '—'}</span>
                  <span>Ingresos: {formatearSoles(s.montoIngresos)}</span>
                  <span>Egresos: {formatearSoles(s.montoEgresos)}</span>
                  <span>Yape: {formatearSoles(s.montoYape)}</span>
                  <span>Plin: {formatearSoles(s.montoPlin)}</span>
                  <span>Efectivo esperado: {formatearSoles(s.efectivoEsperado)}</span>
                  {s.montoCierre !== null && (
                    <span className={(s.diferenciaCierre ?? s.montoCierre - s.efectivoEsperado) === 0 ? '' : 'font-medium text-warning'}>
                      Diferencia: {formatearSoles(s.diferenciaCierre ?? s.montoCierre - s.efectivoEsperado)}
                    </span>
                  )}
                </div>
                {s.observacionCierre && (
                  <p className="mt-1 text-xs"><span className="text-sub">Explicación:</span> {s.observacionCierre}</p>
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
