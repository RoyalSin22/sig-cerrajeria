import { useEffect, useState } from 'react'
import { comprasApi } from '../api/compras'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { formatearSoles } from '../utils/format'
import type { CompraResponse } from '../types/compras'

export function HistorialCompras() {
  const [compras, setCompras] = useState<CompraResponse[]>([])
  const [pagina, setPagina] = useState(0)
  const [hayMas, setHayMas] = useState(false)
  const [cargando, setCargando] = useState(false)

  async function cargar(numero = 0, acumular = false) {
    setCargando(true)
    try {
      const res = await comprasApi.listarCompras(numero)
      setCompras((prev) => (acumular ? [...prev, ...res.content] : res.content))
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
      <h1 className="font-heading text-2xl font-semibold">Historial de compras</h1>

      {cargando && compras.length === 0 ? (
        <p className="text-sm text-sub">Cargando…</p>
      ) : compras.length === 0 ? (
        <EmptyState mensaje="Todavía no hay compras registradas." />
      ) : (
        <>
          <div className="flex flex-col gap-2">
            {compras.map((c) => (
              <Card key={c.id} className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium">{c.producto}</p>
                  <p className="text-xs text-sub">{c.proveedor} · {c.cantidad} und · {c.fecha}</p>
                </div>
                <span className="font-heading font-semibold text-steel-dark">{formatearSoles(c.precioTotal)}</span>
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
