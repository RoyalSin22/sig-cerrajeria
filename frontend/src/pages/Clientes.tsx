import { useEffect, useState, type FormEvent } from 'react'
import { clientesApi } from '../api/clientes'
import { extraerMensajeError } from '../api/axiosClient'
import { SearchInput } from '../components/ui/SearchInput'
import { EmptyState } from '../components/ui/EmptyState'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { BottomSheet } from '../components/ui/BottomSheet'
import { PullToRefreshIndicator } from '../components/ui/PullToRefreshIndicator'
import { usePullToRefresh } from '../hooks/usePullToRefresh'
import { useToast } from '../context/useToast'
import { formatearSoles, formatearFechaHora } from '../utils/format'
import { ETIQUETA_ESTADO_ORDEN } from '../types/ordenes'
import type { ClienteResponse } from '../types/clientes'
import type { VentaResponse } from '../types/ventas'
import type { OrdenTrabajoResponse } from '../types/ordenes'

export function Clientes() {
  const { notificar } = useToast()
  const [resultados, setResultados] = useState<ClienteResponse[]>([])
  const [buscando, setBuscando] = useState(false)
  const [seleccionado, setSeleccionado] = useState<ClienteResponse | null>(null)
  const [mostrarAlta, setMostrarAlta] = useState(false)
  const [pagina, setPagina] = useState(0)
  const [hayMas, setHayMas] = useState(false)
  const [cargando, setCargando] = useState(false)

  async function cargarPagina(numero = 0, acumular = false) {
    setCargando(true)
    try {
      const res = await clientesApi.listar(numero)
      setResultados((prev) => (acumular ? [...prev, ...res.content] : res.content))
      setPagina(res.page.number)
      setHayMas(res.page.number + 1 < res.page.totalPages)
    } finally {
      setCargando(false)
    }
  }

  const { refrescando, distancia, umbral } = usePullToRefresh(() => cargarPagina(0, false))

  useEffect(() => {
    cargarPagina(0, false)
  }, [])

  async function handleBuscar(termino: string) {
    if (termino.trim().length < 2) {
      setBuscando(false)
      cargarPagina(0, false)
      return
    }
    setCargando(true)
    try {
      setResultados(await clientesApi.buscar(termino.trim()))
      setBuscando(true)
      setHayMas(false)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <PullToRefreshIndicator distancia={distancia} refrescando={refrescando} umbral={umbral} />
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-2xl font-semibold">Clientes</h1>
        <Button onClick={() => setMostrarAlta(true)}>Nuevo cliente</Button>
      </div>

      <SearchInput placeholder="Buscar por nombre o teléfono…" onBuscar={handleBuscar} />

      {cargando && resultados.length === 0 ? (
        <p className="text-sm text-sub">Cargando…</p>
      ) : resultados.length === 0 ? (
        <EmptyState mensaje={buscando ? 'No se encontraron clientes con ese criterio.' : 'Todavía no hay clientes registrados.'} />
      ) : (
        <>
          <div className="flex flex-col gap-2">
            {resultados.map((c) => (
              <button key={c.id} onClick={() => setSeleccionado(c)} className="text-left">
                <Card className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium">{c.nombre}</p>
                    <p className="text-xs text-sub">{c.telefono}</p>
                  </div>
                </Card>
              </button>
            ))}
          </div>
          {!buscando && hayMas && (
            <Button variant="secondary" disabled={cargando} onClick={() => cargarPagina(pagina + 1, true)} className="self-center">
              {cargando ? 'Cargando…' : 'Cargar más'}
            </Button>
          )}
        </>
      )}

      <FormularioCliente
        abierto={mostrarAlta}
        onCerrar={() => setMostrarAlta(false)}
        onCreado={(c) => {
          notificar('Cliente registrado')
          setResultados((prev) => [c, ...prev])
          setMostrarAlta(false)
        }}
      />

      <FichaCliente
        cliente={seleccionado}
        onCerrar={() => setSeleccionado(null)}
        onActualizado={(c) => {
          setResultados((prev) => prev.map((x) => (x.id === c.id ? c : x)))
          setSeleccionado(c)
        }}
      />
    </div>
  )
}

function FormularioCliente({
  abierto,
  onCerrar,
  onCreado,
}: {
  abierto: boolean
  onCerrar: () => void
  onCreado: (c: ClienteResponse) => void
}) {
  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')
  const [direccion, setDireccion] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setGuardando(true)
    try {
      const cliente = await clientesApi.crear({ nombre, telefono, direccion: direccion || undefined })
      setNombre('')
      setTelefono('')
      setDireccion('')
      onCreado(cliente)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <BottomSheet abierto={abierto} onCerrar={onCerrar} titulo="Nuevo cliente">
      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <input
          required value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Nombre"
          className="rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel"
        />
        <input
          required value={telefono} onChange={(e) => setTelefono(e.target.value)} placeholder="Teléfono"
          className="rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel"
        />
        <input
          value={direccion} onChange={(e) => setDireccion(e.target.value)} placeholder="Dirección (opcional)"
          className="rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel"
        />
        {error && <p className="text-sm text-danger">{error}</p>}
        <Button type="submit" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</Button>
      </form>
    </BottomSheet>
  )
}

function FichaCliente({
  cliente, onCerrar, onActualizado,
}: { cliente: ClienteResponse | null; onCerrar: () => void; onActualizado: (c: ClienteResponse) => void }) {
  const { notificar } = useToast()
  const [compras, setCompras] = useState<VentaResponse[]>([])
  const [ordenes, setOrdenes] = useState<OrdenTrabajoResponse[]>([])
  const [editando, setEditando] = useState(false)
  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')
  const [direccion, setDireccion] = useState('')
  const [observaciones, setObservaciones] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    if (cliente) {
      clientesApi.historialCompras(cliente.id).then(setCompras).catch(() => setCompras([]))
      clientesApi.historialOrdenes(cliente.id).then(setOrdenes).catch(() => setOrdenes([]))
      setNombre(cliente.nombre)
      setTelefono(cliente.telefono)
      setDireccion(cliente.direccion ?? '')
      setObservaciones(cliente.observaciones ?? '')
      setEditando(false)
      setError(null)
    }
  }, [cliente])

  if (!cliente) return null

  async function guardarEdicion(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setGuardando(true)
    try {
      const actualizado = await clientesApi.actualizar(cliente!.id, {
        nombre, telefono, direccion: direccion || undefined, observaciones: observaciones || undefined,
      })
      notificar('Cliente actualizado')
      onActualizado(actualizado)
      setEditando(false)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  const inputClase = 'w-full rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel'

  return (
    <BottomSheet abierto={!!cliente} onCerrar={onCerrar} titulo={cliente.nombre}>
      {editando ? (
        <form onSubmit={guardarEdicion} className="flex flex-col gap-3 text-sm">
          <input required value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Nombre" className={inputClase} />
          <input required value={telefono} onChange={(e) => setTelefono(e.target.value)} placeholder="Teléfono" className={inputClase} />
          <input value={direccion} onChange={(e) => setDireccion(e.target.value)} placeholder="Dirección" className={inputClase} />
          <textarea value={observaciones} onChange={(e) => setObservaciones(e.target.value)} placeholder="Observaciones" rows={2} className={inputClase} />
          {error && <p className="text-danger">{error}</p>}
          <div className="flex gap-2">
            <Button type="submit" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</Button>
            <Button type="button" variant="secondary" onClick={() => setEditando(false)}>Cancelar</Button>
          </div>
        </form>
      ) : (
        <div className="flex flex-col gap-4 text-sm">
          <div className="flex items-center justify-between">
            <div>
              <span className="text-sub">Teléfono</span>
              <p>{cliente.telefono}</p>
            </div>
            <Button variant="secondary" onClick={() => setEditando(true)}>Editar</Button>
          </div>
          {cliente.direccion && (
            <div><span className="text-sub">Dirección</span><p>{cliente.direccion}</p></div>
          )}
          {cliente.observaciones && (
            <div><span className="text-sub">Observaciones</span><p>{cliente.observaciones}</p></div>
          )}

          <div>
            <span className="font-heading mb-2 block font-semibold">Historial de compras</span>
            {compras.length === 0 ? <p className="text-sub">Sin compras registradas.</p> : (
              <ul className="flex flex-col divide-y divide-hair">
                {compras.map((v) => (
                  <li key={v.id} className="flex justify-between py-2">
                    <span>{formatearFechaHora(v.fechaHora)}</span>
                    <span className="font-medium">{formatearSoles(v.montoTotal)}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div>
            <span className="font-heading mb-2 block font-semibold">Historial de órdenes</span>
            {ordenes.length === 0 ? <p className="text-sub">Sin órdenes registradas.</p> : (
              <ul className="flex flex-col divide-y divide-hair">
                {ordenes.map((o) => (
                  <li key={o.id} className="flex justify-between py-2">
                    <span>{o.numeroOrden}</span>
                    <span className="text-sub">{ETIQUETA_ESTADO_ORDEN[o.estado]}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </BottomSheet>
  )
}
