import { useEffect, useState } from 'react'
import { auditoriaApi } from '../api/auditoria'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { formatearFechaHora } from '../utils/format'
import type { AuditLogResponse } from '../types/auditoria'

const ETIQUETA_ACCION: Record<string, string> = {
  CREAR: 'Creación',
  EDITAR: 'Edición',
  ELIMINAR: 'Eliminación',
  CAMBIO_ESTADO: 'Cambio de estado',
  AJUSTE_INVENTARIO: 'Ajuste de inventario',
  ANULACION: 'Anulación',
  APERTURA_CAJA: 'Apertura de caja',
  CIERRE_CAJA: 'Cierre de caja',
}

/** RF-47: registro de auditoría, visible solo para ADMIN (ver ProtectedRoute en App.tsx). */
export function Auditoria() {
  const [registros, setRegistros] = useState<AuditLogResponse[]>([])
  const [pagina, setPagina] = useState(0)
  const [hayMas, setHayMas] = useState(false)
  const [cargando, setCargando] = useState(false)

  async function cargar(numero = 0, acumular = false) {
    setCargando(true)
    try {
      const res = await auditoriaApi.listar(numero)
      setRegistros((prev) => (acumular ? [...prev, ...res.content] : res.content))
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
      <h1 className="font-heading text-2xl font-semibold">Auditoría</h1>

      {cargando && registros.length === 0 ? (
        <p className="text-sm text-sub">Cargando…</p>
      ) : registros.length === 0 ? (
        <EmptyState mensaje="Todavía no hay actividad registrada." />
      ) : (
        <>
          <Card>
            <ul className="flex flex-col divide-y divide-hair text-sm">
              {registros.map((r) => (
                <li key={r.id} className="flex items-center justify-between py-2.5">
                  <div>
                    <p className="font-medium">{ETIQUETA_ACCION[r.accion] ?? r.accion}</p>
                    <p className="text-xs text-sub">{r.entidad} #{r.entidadId}</p>
                  </div>
                  <div className="text-right text-xs text-sub">
                    <p>{r.usuario}</p>
                    <p>{formatearFechaHora(r.fechaHora)}</p>
                  </div>
                </li>
              ))}
            </ul>
          </Card>
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
