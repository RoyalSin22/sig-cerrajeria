import { useEffect, useState } from 'react'
import { auditoriaApi } from '../api/auditoria'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { formatearFechaHora, formatearSoles } from '../utils/format'
import type { AuditLogResponse } from '../types/auditoria'

const ETIQUETA_ACCION: Record<string, string> = {
  CREAR: 'Creación',
  EDITAR: 'Edición',
  ELIMINAR: 'Eliminación',
  CAMBIO_ESTADO: 'Cambio de estado',
  AJUSTE_INVENTARIO: 'Ajuste de inventario',
  ANULACION: 'Anulación',
  CAMBIO_PASSWORD: 'Cambio de contraseña',
  APERTURA_CAJA: 'Apertura de caja',
  CIERRE_CAJA: 'Cierre de caja',
  MOVIMIENTO_CAJA: 'Movimiento manual de caja',
  GARANTIA: 'Garantía',
}

/** Nombres legibles de los campos que guarda el backend en "cambios". */
const ETIQUETA_CAMPO: Record<string, string> = {
  stock: 'Stock',
  precioCompra: 'Precio de compra',
  precioVenta: 'Precio de venta',
  precioDuplicado: 'Precio del duplicado',
  stockMinimo: 'Stock mínimo',
  nombre: 'Nombre',
  marca: 'Marca',
  estado: 'Estado',
  activo: 'Activo',
  rol: 'Rol',
  montoApertura: 'Monto de apertura',
  efectivoEsperado: 'Efectivo esperado',
  efectivoContado: 'Efectivo contado',
}

const CAMPOS_EN_SOLES = new Set([
  'precioCompra', 'precioVenta', 'precioDuplicado', 'montoApertura', 'efectivoEsperado', 'efectivoContado',
])

function valor(campo: string, texto: string | null) {
  if (texto === null) return '—'
  if (CAMPOS_EN_SOLES.has(campo)) return formatearSoles(Number(texto))
  if (texto === 'true') return 'sí'
  if (texto === 'false') return 'no'
  return texto
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
      <h1 className="font-heading text-3xl font-bold">Auditoría</h1>

      {cargando && registros.length === 0 ? (
        <p className="text-sm text-sub">Cargando…</p>
      ) : registros.length === 0 ? (
        <EmptyState mensaje="Todavía no hay actividad registrada." />
      ) : (
        <>
          <Card>
            <ul className="flex flex-col divide-y divide-hair text-sm">
              {registros.map((r) => (
                <li key={r.id} className="flex items-start justify-between gap-3 py-2.5">
                  <div className="min-w-0">
                    <p className="font-medium">{ETIQUETA_ACCION[r.accion] ?? r.accion}</p>
                    <p className="text-xs text-sub">{r.detalle?.descripcion ?? `${r.entidad} #${r.entidadId}`}</p>
                    {r.detalle?.motivo && <p className="text-xs"><span className="text-sub">Motivo:</span> {r.detalle.motivo}</p>}
                    {r.detalle?.cambios && (
                      <ul className="mt-0.5 text-xs">
                        {Object.entries(r.detalle.cambios).map(([campo, c]) => (
                          <li key={campo}>
                            <span className="text-sub">{ETIQUETA_CAMPO[campo] ?? campo}:</span>{' '}
                            {c.antes !== null && <>{valor(campo, c.antes)} → </>}
                            <b>{valor(campo, c.despues)}</b>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                  <div className="shrink-0 text-right text-xs text-sub">
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
