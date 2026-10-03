import { useEffect, useState, type FormEvent } from 'react'
import { domicilioApi } from '../api/domicilio'
import { clientesApi } from '../api/clientes'
import { extraerMensajeError } from '../api/axiosClient'
import { Card } from '../components/ui/Card'
import { Chip } from '../components/ui/Chip'
import { PullToRefreshIndicator } from '../components/ui/PullToRefreshIndicator'
import { usePullToRefresh } from '../hooks/usePullToRefresh'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { BottomSheet } from '../components/ui/BottomSheet'
import { SearchInput } from '../components/ui/SearchInput'
import { EmptyState } from '../components/ui/EmptyState'
import { useToast } from '../context/ToastContext'
import { ETIQUETA_ESTADO_SERVICIO, type EstadoServicio, type ServicioDomicilioResponse } from '../types/domicilio'
import type { ClienteResponse } from '../types/clientes'

const TONO_ESTADO: Record<EstadoServicio, 'neutral' | 'steel' | 'success' | 'warning'> = {
  PROGRAMADO: 'neutral',
  EN_CAMINO: 'steel',
  REALIZADO: 'success',
  CANCELADO: 'warning',
}

function hoyISO() {
  return new Date().toISOString().slice(0, 10)
}

function sumarDiasISO(fechaISO: string, dias: number) {
  const d = new Date(fechaISO)
  d.setDate(d.getDate() + dias)
  return d.toISOString().slice(0, 10)
}

function sumarMesesISO(fechaISO: string, meses: number) {
  const d = new Date(fechaISO)
  d.setMonth(d.getMonth() + meses)
  return d.toISOString().slice(0, 10)
}

function inicioMesISO(fechaISO: string) {
  const d = new Date(fechaISO)
  return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10)
}

function finMesISO(fechaISO: string) {
  const d = new Date(fechaISO)
  return new Date(d.getFullYear(), d.getMonth() + 1, 0).toISOString().slice(0, 10)
}

type Vista = 'SEMANA' | 'MES'

export function Domicilio() {
  const { notificar } = useToast()
  const [vista, setVista] = useState<Vista>('SEMANA')
  const [desde, setDesde] = useState(hoyISO())
  const [servicios, setServicios] = useState<ServicioDomicilioResponse[]>([])
  const [mostrarAlta, setMostrarAlta] = useState(false)

  const hasta = vista === 'SEMANA' ? sumarDiasISO(desde, 6) : finMesISO(desde)
  const rangoDesde = vista === 'SEMANA' ? desde : inicioMesISO(desde)

  async function cargar() {
    setServicios(await domicilioApi.agenda(rangoDesde, hasta))
  }

  const { refrescando, distancia, umbral } = usePullToRefresh(cargar)

  useEffect(() => {
    cargar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [desde, vista])

  async function cambiarEstado(id: number, nuevoEstado: EstadoServicio) {
    const actualizado = await domicilioApi.cambiarEstado(id, nuevoEstado)
    setServicios((prev) => prev.map((s) => (s.id === id ? actualizado : s)))
  }

  return (
    <div className="flex flex-col gap-4">
      <PullToRefreshIndicator distancia={distancia} refrescando={refrescando} umbral={umbral} />
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-2xl font-semibold">Domicilio</h1>
        <Button onClick={() => setMostrarAlta(true)}>Agendar servicio</Button>
      </div>

      <div className="flex gap-2">
        <Chip activo={vista === 'SEMANA'} onClick={() => setVista('SEMANA')}>Semana</Chip>
        <Chip activo={vista === 'MES'} onClick={() => setVista('MES')}>Mes</Chip>
      </div>

      <div className="flex items-center gap-2">
        <Button
          variant="secondary"
          onClick={() => setDesde((d) => (vista === 'SEMANA' ? sumarDiasISO(d, -7) : sumarMesesISO(d, -1)))}
        >
          ◀ Anterior
        </Button>
        <span className="text-sm text-sub">{rangoDesde} → {hasta}</span>
        <Button
          variant="secondary"
          onClick={() => setDesde((d) => (vista === 'SEMANA' ? sumarDiasISO(d, 7) : sumarMesesISO(d, 1)))}
        >
          Siguiente ▶
        </Button>
      </div>

      {servicios.length === 0 ? (
        <EmptyState mensaje="No hay servicios programados en este rango." />
      ) : (
        <div className="flex flex-col gap-2">
          {servicios.map((s) => (
            <Card key={s.id} className="flex items-center justify-between gap-3">
              <div>
                <p className="text-sm font-medium">{s.fecha} · {s.hora}</p>
                <p className="text-sm">{s.cliente} — {s.tipoServicio}</p>
                <p className="text-xs text-sub">{s.direccion} · {s.telefono}</p>
              </div>
              <div className="flex flex-col items-end gap-2">
                <Badge tono={TONO_ESTADO[s.estado]}>{ETIQUETA_ESTADO_SERVICIO[s.estado]}</Badge>
                {s.estado === 'PROGRAMADO' && (
                  <button onClick={() => cambiarEstado(s.id, 'EN_CAMINO')} className="text-xs text-steel underline">
                    Marcar en camino
                  </button>
                )}
                {s.estado === 'EN_CAMINO' && (
                  <button onClick={() => cambiarEstado(s.id, 'REALIZADO')} className="text-xs text-steel underline">
                    Marcar realizado
                  </button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      <FormularioServicio
        abierto={mostrarAlta}
        onCerrar={() => setMostrarAlta(false)}
        onCreado={(s) => {
          notificar('Servicio agendado')
          setServicios((prev) => [...prev, s])
          setMostrarAlta(false)
        }}
      />
    </div>
  )
}

function FormularioServicio({
  abierto, onCerrar, onCreado,
}: { abierto: boolean; onCerrar: () => void; onCreado: (s: ServicioDomicilioResponse) => void }) {
  const [clientesEncontrados, setClientesEncontrados] = useState<ClienteResponse[]>([])
  const [clienteId, setClienteId] = useState<number | null>(null)
  const [clienteNombre, setClienteNombre] = useState('')
  const [direccion, setDireccion] = useState('')
  const [telefono, setTelefono] = useState('')
  const [fecha, setFecha] = useState(hoyISO())
  const [hora, setHora] = useState('09:00')
  const [tipoServicio, setTipoServicio] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  async function buscarCliente(termino: string) {
    if (termino.trim().length >= 2) {
      const encontrados = await clientesApi.buscar(termino.trim())
      setClientesEncontrados(encontrados)
    } else {
      setClientesEncontrados([])
    }
  }

  function seleccionarCliente(c: ClienteResponse) {
    setClienteId(c.id)
    setClienteNombre(c.nombre)
    setTelefono(c.telefono)
    if (c.direccion) setDireccion(c.direccion)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!clienteId) {
      setError('Selecciona un cliente de la lista')
      return
    }
    setError(null)
    setGuardando(true)
    try {
      const servicio = await domicilioApi.crear({ clienteId, direccion, telefono, fecha, hora, tipoServicio })
      onCreado(servicio)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <BottomSheet abierto={abierto} onCerrar={onCerrar} titulo="Agendar servicio a domicilio">
      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        {!clienteId ? (
          <>
            <SearchInput placeholder="Buscar cliente…" onBuscar={buscarCliente} />
            {clientesEncontrados.map((c) => (
              <button key={c.id} type="button" onClick={() => seleccionarCliente(c)}
                className="rounded-md border border-line p-2 text-left text-sm hover:bg-hair">
                {c.nombre} · {c.telefono}
              </button>
            ))}
          </>
        ) : (
          <div className="flex items-center justify-between rounded-md bg-tint-sky p-2 text-sm">
            <span>{clienteNombre}</span>
            <button type="button" onClick={() => setClienteId(null)} className="text-steel underline">Cambiar</button>
          </div>
        )}

        <input required value={direccion} onChange={(e) => setDireccion(e.target.value)} placeholder="Dirección"
          className="rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel" />
        <input required value={telefono} onChange={(e) => setTelefono(e.target.value)} placeholder="Teléfono"
          className="rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel" />
        <div className="flex gap-2">
          <input required type="date" value={fecha} onChange={(e) => setFecha(e.target.value)}
            className="flex-1 rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel" />
          <input required type="time" value={hora} onChange={(e) => setHora(e.target.value)}
            className="flex-1 rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel" />
        </div>
        <input required value={tipoServicio} onChange={(e) => setTipoServicio(e.target.value)}
          placeholder="Tipo de servicio (ej. Cambio de chapa)"
          className="rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel" />

        {error && <p className="text-sm text-danger">{error}</p>}
        <Button type="submit" disabled={guardando}>{guardando ? 'Agendando…' : 'Agendar'}</Button>
      </form>
    </BottomSheet>
  )
}
