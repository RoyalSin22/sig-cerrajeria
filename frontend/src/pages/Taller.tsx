import { useEffect, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { MessageCircle } from 'lucide-react'
import { ordenesApi } from '../api/ordenes'
import { clientesApi } from '../api/clientes'
import { productosApi } from '../api/productos'
import { useNegocio } from '../hooks/useNegocio'
import { enlaceWhatsApp } from '../utils/whatsapp'
import { extraerMensajeError } from '../api/axiosClient'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { BottomSheet } from '../components/ui/BottomSheet'
import { PullToRefreshIndicator } from '../components/ui/PullToRefreshIndicator'
import { usePullToRefresh } from '../hooks/usePullToRefresh'
import { SearchInput } from '../components/ui/SearchInput'
import { Campo } from '../components/ui/Campo'
import { useToast } from '../context/useToast'
import { formatearSoles } from '../utils/format'
import { ESTADOS_ORDEN, ETIQUETA_ESTADO_ORDEN, type EstadoOrden, type OrdenTrabajoResponse } from '../types/ordenes'
import { ETIQUETA_METODO_PAGO, METODOS_EN_USO, type MetodoPago } from '../types/ventas'
import type { ClienteResponse } from '../types/clientes'
import type { MaterialOrdenResponse } from '../types/ordenes'
import type { ProductoResponse } from '../types/productos'

const TONO_ESTADO: Record<EstadoOrden, 'neutral' | 'steel' | 'warning' | 'success' | 'dark'> = {
  RECIBIDO: 'neutral',
  EN_REPARACION: 'steel',
  ESPERANDO_REPUESTO: 'warning',
  LISTO: 'success',
  ENTREGADO: 'dark',
  CANCELADO: 'neutral',
}

export function Taller() {
  const { notificar } = useToast()
  const [ordenes, setOrdenes] = useState<OrdenTrabajoResponse[]>([])
  const [seleccionada, setSeleccionada] = useState<OrdenTrabajoResponse | null>(null)
  const [verTodas, setVerTodas] = useState(false)
  // Desde Inicio, "Recibir trabajo" llega con ?nueva=1 y abre el formulario directo
  const [params, setParams] = useSearchParams()
  const [mostrarAlta, setMostrarAlta] = useState(params.get('nueva') === '1')

  async function cargar() {
    setOrdenes(await ordenesApi.listar())
  }

  const { refrescando, distancia, umbral } = usePullToRefresh(cargar)

  useEffect(() => {
    cargar()
  }, [])

  function actualizarEnLista(orden: OrdenTrabajoResponse) {
    setOrdenes((prev) => prev.map((o) => (o.id === orden.id ? orden : o)))
    setSeleccionada(orden)
  }

  return (
    <div className="flex flex-col gap-4">
      <PullToRefreshIndicator distancia={distancia} refrescando={refrescando} umbral={umbral} />
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-3xl font-bold">Taller</h1>
        <Button tamano="lg" onClick={() => setMostrarAlta(true)}>Recibir trabajo</Button>
      </div>

      {/* Kanban por estado (Fase 7 §5.6). En el celular las columnas van una debajo
          de otra: deslizar hacia abajo es mas facil que hacia el costado. */}
      <div className="flex flex-col gap-5 md:flex-row md:gap-3 md:overflow-x-auto md:pb-2">
        {[...ESTADOS_ORDEN, ...(ordenes.some((o) => o.estado === 'CANCELADO') ? ['CANCELADO' as const] : [])].map((estado) => {
          const delEstado = ordenes.filter((o) => o.estado === estado)
          // Las terminadas se acumulan: se muestran las 5 ultimas y el resto a pedido
          const terminada = estado === 'ENTREGADO' || estado === 'CANCELADO'
          const visibles = terminada && !verTodas ? delEstado.slice(0, 5) : delEstado
          return (
          <div key={estado} className="md:w-72 md:shrink-0">
            <div className="mb-2 flex items-center gap-2">
              <span className="font-heading text-xl font-bold">{ETIQUETA_ESTADO_ORDEN[estado]}</span>
              <span className="text-base text-sub">({delEstado.length})</span>
            </div>
            <div className="flex flex-col gap-2">
              {delEstado.length === 0 && <p className="text-base text-sub">Ninguna.</p>}
              {visibles.map((o) => (
                <button key={o.id} onClick={() => setSeleccionada(o)} className="text-left">
                  <Card className="flex flex-col gap-1">
                    <span className="flex items-center justify-between gap-2 text-xs text-sub">
                      {o.numeroOrden}
                      {o.garantia && <Badge tono="steel">Garantía</Badge>}
                    </span>
                    <span className="text-lg font-semibold">{o.cliente}</span>
                    <span className="text-base text-sub">{o.productoRecibido}</span>
                    {o.saldoPendiente > 0 && (
                      <Badge tono="warning">Debe {formatearSoles(o.saldoPendiente)}</Badge>
                    )}
                  </Card>
                </button>
              ))}
              {visibles.length < delEstado.length && (
                <Button variant="secondary" onClick={() => setVerTodas(true)}>
                  Ver las {delEstado.length - visibles.length} anteriores
                </Button>
              )}
            </div>
          </div>
          )
        })}
      </div>

      <FormularioOrden
        abierto={mostrarAlta}
        onCerrar={() => {
          setMostrarAlta(false)
          if (params.has('nueva')) setParams({}, { replace: true })
        }}
        onCreada={(o) => {
          notificar('Orden creada: ' + o.numeroOrden)
          setOrdenes((prev) => [o, ...prev])
          setMostrarAlta(false)
          setSeleccionada(o) // queda a mano el boton para mandarle el comprobante por WhatsApp
        }}
      />

      <FichaOrden
        key={seleccionada?.id}
        orden={seleccionada}
        onCerrar={() => setSeleccionada(null)}
        onActualizada={actualizarEnLista}
        onGarantia={(g) => {
          notificar(`Garantía abierta: ${g.numeroOrden}`)
          setOrdenes((prev) => [g, ...prev])
          setSeleccionada(g)
        }}
      />
    </div>
  )
}

function FormularioOrden({
  abierto, onCerrar, onCreada,
}: { abierto: boolean; onCerrar: () => void; onCreada: (o: OrdenTrabajoResponse) => void }) {
  const [clientesEncontrados, setClientesEncontrados] = useState<ClienteResponse[]>([])
  const [clienteId, setClienteId] = useState<number | null>(null)
  const [clienteNombre, setClienteNombre] = useState('')
  const [productoRecibido, setProductoRecibido] = useState('')
  const [problema, setProblema] = useState('')
  const [precioTotal, setPrecioTotal] = useState('')
  const [adelanto, setAdelanto] = useState('')
  const [metodoPago, setMetodoPago] = useState<MetodoPago>('EFECTIVO')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  async function buscarCliente(termino: string) {
    if (termino.trim().length >= 2) {
      setClientesEncontrados(await clientesApi.buscar(termino.trim()))
    } else {
      setClientesEncontrados([])
    }
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
      const orden = await ordenesApi.crear({
        clienteId,
        productoRecibido,
        problema,
        precioTotal: Number(precioTotal),
        adelanto: adelanto ? Number(adelanto) : undefined,
        metodoPagoAdelanto: adelanto ? metodoPago : undefined,
      })
      onCreada(orden)
      setClienteId(null); setClienteNombre(''); setProductoRecibido(''); setProblema(''); setPrecioTotal(''); setAdelanto('')
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <BottomSheet abierto={abierto} onCerrar={onCerrar} titulo="Nueva orden de trabajo">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-2">
          <span className="text-base font-semibold">1. Cliente</span>
          {!clienteId ? (
            <>
              <SearchInput placeholder="Escribe el nombre o teléfono…" onBuscar={buscarCliente} />
              {clientesEncontrados.map((c) => (
                <button
                  key={c.id} type="button"
                  onClick={() => { setClienteId(c.id); setClienteNombre(c.nombre) }}
                  className="min-h-12 rounded-lg border-2 border-hair bg-card px-3 py-2 text-left text-base hover:border-steel"
                >
                  <b>{c.nombre}</b> · {c.telefono}
                </button>
              ))}
            </>
          ) : (
            <div className="flex items-center justify-between gap-2 rounded-lg bg-tint-sky p-3 text-lg font-semibold">
              <span>{clienteNombre}</span>
              <Button type="button" variant="secondary" onClick={() => setClienteId(null)}>Cambiar</Button>
            </div>
          )}
        </div>

        <Campo etiqueta="2. ¿Qué trajo?">
          <input required value={productoRecibido} onChange={(e) => setProductoRecibido(e.target.value)}
            placeholder="Ej.: chapa Yale de la puerta principal" autoCapitalize="sentences"
            className="rounded-lg border px-3 outline-none" />
        </Campo>
        <Campo etiqueta="3. ¿Qué problema tiene?">
          <textarea required value={problema} onChange={(e) => setProblema(e.target.value)}
            placeholder="Ej.: no gira la llave" rows={2} autoCapitalize="sentences"
            className="rounded-lg border px-3 py-2 outline-none" />
        </Campo>
        <div className="grid grid-cols-2 items-end gap-3">
          <Campo etiqueta="4. Precio (S/)">
            <input required type="number" step="0.01" min="0" inputMode="decimal" value={precioTotal}
              onChange={(e) => setPrecioTotal(e.target.value)}
              className="rounded-lg border px-3 text-lg outline-none" />
          </Campo>
          <Campo etiqueta="Adelanto (S/)">
            <input type="number" step="0.01" min="0" inputMode="decimal" value={adelanto}
              onChange={(e) => setAdelanto(e.target.value)}
              className="rounded-lg border px-3 text-lg outline-none" />
          </Campo>
        </div>
        {adelanto && (
          <Campo etiqueta="¿Cómo pagó el adelanto?">
            <select value={metodoPago} onChange={(e) => setMetodoPago(e.target.value as MetodoPago)}
              className="rounded-lg border px-3 outline-none">
              {METODOS_EN_USO.map((m) => <option key={m} value={m}>{ETIQUETA_METODO_PAGO[m]}</option>)}
            </select>
          </Campo>
        )}

        {error && <p className="text-base font-semibold text-danger">{error}</p>}
        <Button type="submit" tamano="lg" disabled={guardando}>{guardando ? 'Guardando…' : 'Crear orden'}</Button>
      </form>
    </BottomSheet>
  )
}

function FichaOrden({
  orden, onCerrar, onActualizada, onGarantia,
}: {
  orden: OrdenTrabajoResponse | null
  onCerrar: () => void
  onActualizada: (o: OrdenTrabajoResponse) => void
  onGarantia: (garantia: OrdenTrabajoResponse) => void
}) {
  const { notificar } = useToast()
  const [error, setError] = useState<string | null>(null)
  const [procesando, setProcesando] = useState(false)
  const [montoLiquidacion, setMontoLiquidacion] = useState('')
  const [metodoPago, setMetodoPago] = useState<MetodoPago>('EFECTIVO')
  const [cancelando, setCancelando] = useState(false)
  const [motivo, setMotivo] = useState('')
  const [devolver, setDevolver] = useState(true)
  const [abriendoGarantia, setAbriendoGarantia] = useState(false)
  const [problemaGarantia, setProblemaGarantia] = useState('')

  if (!orden) return null

  async function abrirGarantia() {
    if (!problemaGarantia.trim()) {
      setError('Describe qué falló')
      return
    }
    setError(null)
    setProcesando(true)
    try {
      onGarantia(await ordenesApi.garantia(orden!.id, { problema: problemaGarantia.trim() }))
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setProcesando(false)
    }
  }

  async function cancelarOrden() {
    if (!motivo.trim()) {
      setError('Escribe el motivo de la cancelación')
      return
    }
    setError(null)
    setProcesando(true)
    try {
      const devuelve = devolver && orden!.montoPagado > 0
      const actualizada = await ordenesApi.cancelar(orden!.id, {
        motivo, devolverAdelanto: devuelve, metodoDevolucion: devuelve ? metodoPago : undefined,
      })
      notificar(devuelve
        ? `Orden ${actualizada.numeroOrden} cancelada; devuelto ${formatearSoles(orden!.montoPagado)} desde tu caja`
        : `Orden ${actualizada.numeroOrden} cancelada`)
      setCancelando(false)
      setMotivo('')
      onActualizada(actualizada)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setProcesando(false)
    }
  }

  async function cambiarEstado(nuevoEstado: typeof ESTADOS_ORDEN[number]) {
    setError(null)
    setProcesando(true)
    try {
      const actualizada = await ordenesApi.cambiarEstado(orden!.id, { nuevoEstado })
      notificar(`Orden ${actualizada.numeroOrden}: ${ETIQUETA_ESTADO_ORDEN[nuevoEstado]}`)
      onActualizada(actualizada)
    } catch (err) {
      setError(extraerMensajeError(err)) // aqui aparece el bloqueo de la regla de negocio 3
    } finally {
      setProcesando(false)
    }
  }

  async function entregarConLiquidacion() {
    setError(null)
    setProcesando(true)
    try {
      const actualizada = await ordenesApi.entregar(
        orden!.id,
        orden!.saldoPendiente > 0 ? { monto: Number(montoLiquidacion), metodoPago } : undefined,
      )
      notificar(`Orden ${actualizada.numeroOrden} entregada`)
      onActualizada(actualizada)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setProcesando(false)
    }
  }

  const siguienteEstadoSimple = ESTADOS_ORDEN[ESTADOS_ORDEN.indexOf(orden.estado) + 1]

  return (
    <BottomSheet abierto={!!orden} onCerrar={onCerrar} titulo={`${orden.numeroOrden} — ${orden.cliente}`}>
      <div className="flex flex-col gap-4 text-sm">
        <div className="flex flex-wrap gap-2">
          <Badge tono={TONO_ESTADO[orden.estado]}>{ETIQUETA_ESTADO_ORDEN[orden.estado]}</Badge>
          {orden.garantia && <Badge tono="steel">Garantía de {orden.numeroOrdenOrigen} · sin cobro</Badge>}
        </div>

        <div><span className="text-sub">Producto</span><p>{orden.productoRecibido}</p></div>
        <div><span className="text-sub">Problema</span><p>{orden.problema}</p></div>

        <div className="grid grid-cols-2 gap-2">
          <div><span className="text-sub">Precio total</span><p>{formatearSoles(orden.precioTotal)}</p></div>
          <div><span className="text-sub">Pagado</span><p>{formatearSoles(orden.montoPagado)}</p></div>
        </div>
        <div className={orden.saldoPendiente > 0 ? 'rounded-md bg-tint-peach p-2 font-semibold text-danger' : ''}>
          Saldo pendiente: {formatearSoles(orden.saldoPendiente)}
        </div>

        {orden.estado === 'CANCELADO' && orden.motivoCancelacion && (
          <div><span className="text-sub">Motivo de la cancelación</span><p>{orden.motivoCancelacion}</p></div>
        )}

        {orden.estado !== 'CANCELADO' && <AvisosWhatsApp orden={orden} />}

        <MaterialesOrden ordenId={orden.id} editable={orden.estado !== 'ENTREGADO' && orden.estado !== 'CANCELADO'} />

        {error && <p className="text-base font-semibold text-danger">{error}</p>}

        {orden.estado === 'ENTREGADO' && (
          <div className="flex flex-col gap-2 border-t border-hair pt-3">
            {!abriendoGarantia ? (
              <Button variant="secondary" onClick={() => setAbriendoGarantia(true)}>Garantía</Button>
            ) : (
              <div className="flex flex-col gap-2 rounded-md border border-line p-3">
                <span className="font-medium">El cliente volvió: abrir garantía</span>
                <p className="text-xs text-sub">
                  Se crea una orden nueva sin cobro, ligada a esta. El material que uses sale del stock.
                </p>
                <textarea
                  rows={2} value={problemaGarantia} onChange={(e) => setProblemaGarantia(e.target.value)} maxLength={1000}
                  placeholder="¿Qué falló? (ej. la chapa volvió a trabarse)"
                  className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel"
                />
                <div className="flex gap-2">
                  <Button disabled={procesando} onClick={abrirGarantia}>Abrir garantía</Button>
                  <Button variant="secondary" disabled={procesando} onClick={() => setAbriendoGarantia(false)}>Volver</Button>
                </div>
              </div>
            )}
          </div>
        )}

        {orden.estado !== 'ENTREGADO' && orden.estado !== 'CANCELADO' && (
          <div className="flex flex-col gap-2 border-t border-hair pt-3">
            {siguienteEstadoSimple && siguienteEstadoSimple !== 'ENTREGADO' && (
              <Button variant="secondary" disabled={procesando} onClick={() => cambiarEstado(siguienteEstadoSimple)}>
                Avanzar a: {ETIQUETA_ESTADO_ORDEN[siguienteEstadoSimple]}
              </Button>
            )}

            {orden.saldoPendiente > 0 ? (
              <div className="flex flex-col gap-2 rounded-md border border-warning/40 bg-tint-peach p-3">
                <span className="font-medium">Entregar con liquidación del saldo</span>
                <input
                  type="number" inputMode="decimal" step="0.01" value={montoLiquidacion} onChange={(e) => setMontoLiquidacion(e.target.value)}
                  placeholder={`Monto (mínimo ${orden.saldoPendiente})`}
                  className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel"
                />
                <select value={metodoPago} onChange={(e) => setMetodoPago(e.target.value as MetodoPago)}
                  className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel">
                  {METODOS_EN_USO.map((m) => <option key={m} value={m}>{ETIQUETA_METODO_PAGO[m]}</option>)}
                </select>
                <Button disabled={procesando} onClick={entregarConLiquidacion}>Liquidar y entregar</Button>
              </div>
            ) : (
              <Button disabled={procesando} onClick={entregarConLiquidacion}>Marcar como entregado</Button>
            )}

            {!cancelando ? (
              <Button variant="ghost" disabled={procesando} onClick={() => setCancelando(true)}>Cancelar orden</Button>
            ) : (
              <div className="flex flex-col gap-2 rounded-md border border-line p-3">
                <span className="font-medium">Cancelar la orden</span>
                <input
                  value={motivo} onChange={(e) => setMotivo(e.target.value)} maxLength={500}
                  placeholder="Motivo (ej. el cliente se arrepintió, nunca la recogió)"
                  className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel"
                />
                {orden.montoPagado > 0 && (
                  <>
                    <label className="flex items-center gap-2">
                      <input type="checkbox" checked={devolver} onChange={(e) => setDevolver(e.target.checked)} />
                      Devolver lo cobrado ({formatearSoles(orden.montoPagado)}) desde mi caja
                    </label>
                    {devolver && (
                      <select value={metodoPago} onChange={(e) => setMetodoPago(e.target.value as MetodoPago)}
                        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel">
                        {METODOS_EN_USO.map((m) => <option key={m} value={m}>Devolver por {ETIQUETA_METODO_PAGO[m]}</option>)}
                      </select>
                    )}
                  </>
                )}
                <div className="flex gap-2">
                  <Button variant="danger" disabled={procesando} onClick={cancelarOrden}>Confirmar cancelación</Button>
                  <Button variant="secondary" disabled={procesando} onClick={() => setCancelando(false)}>Volver</Button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </BottomSheet>
  )
}

/**
 * Material del inventario usado en la orden (una pieza, un cilindro): sale del
 * stock por el Kárdex. En una garantía es la forma de registrar lo que se usó
 * sin cobrarlo.
 */
function MaterialesOrden({ ordenId, editable }: { ordenId: number; editable: boolean }) {
  const [materiales, setMateriales] = useState<MaterialOrdenResponse[]>([])
  const [encontrados, setEncontrados] = useState<ProductoResponse[]>([])
  const [elegido, setElegido] = useState<ProductoResponse | null>(null)
  const [cantidad, setCantidad] = useState('1')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    ordenesApi.materiales(ordenId).then(setMateriales).catch(() => setMateriales([]))
  }, [ordenId])

  async function buscar(termino: string) {
    setEncontrados(termino.trim().length >= 2 ? await productosApi.buscar(termino.trim()) : [])
  }

  async function usar() {
    const n = Number.parseInt(cantidad, 10)
    if (!elegido || !(n >= 1)) return
    setError(null)
    setGuardando(true)
    try {
      setMateriales(await ordenesApi.usarMaterial(ordenId, elegido.id, n))
      setElegido(null)
      setCantidad('1')
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  if (!editable && materiales.length === 0) return null

  return (
    <div className="flex flex-col gap-2 border-t border-hair pt-3">
      <span className="font-medium">Material usado</span>
      {materiales.length === 0 ? (
        <p className="text-xs text-sub">Ninguno todavía.</p>
      ) : (
        <ul className="flex flex-col gap-1">
          {materiales.map((m, i) => (
            <li key={i} className="flex justify-between gap-2 text-xs">
              <span>{m.cantidad} × {m.nombre} <span className="text-sub">({m.codigo})</span></span>
              <span className="text-sub">{m.usuario}</span>
            </li>
          ))}
        </ul>
      )}
      {editable && (
        elegido ? (
          <div className="flex items-center gap-2">
            <span className="min-w-0 flex-1 truncate text-xs">{elegido.nombre} · hay {elegido.cantidadStock}</span>
            <input
              type="number" min="1" inputMode="numeric" value={cantidad} onChange={(e) => setCantidad(e.target.value)}
              aria-label="Cantidad usada"
              className="w-16 rounded-md border border-line bg-paper px-2 py-1.5 text-right text-sm outline-none focus:border-steel"
            />
            <Button disabled={guardando} onClick={usar} className="px-3">Usar</Button>
            <button type="button" onClick={() => setElegido(null)} className="text-xs text-steel underline">Cambiar</button>
          </div>
        ) : (
          <>
            <SearchInput placeholder="Agregar material del inventario…" onBuscar={buscar} />
            {encontrados.length > 0 && (
              <div className="flex flex-col gap-1">
                {encontrados.slice(0, 6).map((p) => (
                  <button key={p.id} type="button" onClick={() => { setElegido(p); setEncontrados([]) }}
                    className="flex justify-between rounded-md border border-hair px-2 py-1.5 text-left text-xs hover:bg-hair">
                    <span>{p.nombre}</span>
                    <span className="text-sub">hay {p.cantidadStock}</span>
                  </button>
                ))}
              </div>
            )}
          </>
        )
      )}
      {error && <p className="text-xs text-danger">{error}</p>}
    </div>
  )
}

/**
 * Mensajes al cliente por WhatsApp, ya escritos con los datos del negocio: el
 * comprobante de lo que dejo y, cuando esta listo, el aviso para que venga.
 * Abre el WhatsApp del celular (o WhatsApp Web en la PC); se envia desde ahi.
 */
function AvisosWhatsApp({ orden }: { orden: OrdenTrabajoResponse }) {
  const negocio = useNegocio()
  const contacto = `${negocio.nombre}\n${negocio.direccion}\nTel. ${negocio.telefono}`
  const debe = orden.saldoPendiente > 0 ? `Saldo: ${formatearSoles(orden.saldoPendiente)}.` : 'Ya está pagado.'

  const comprobante = [
    `Hola ${orden.cliente}, este es su comprobante de ${negocio.nombre}.`,
    `Orden ${orden.numeroOrden}: ${orden.productoRecibido}`,
    `Problema: ${orden.problema}`,
    orden.garantia
      ? `Es garantía de la orden ${orden.numeroOrdenOrigen}: sin costo.`
      : `Precio: ${formatearSoles(orden.precioTotal)} · Dejó: ${formatearSoles(orden.montoPagado)} · ${debe}`,
    'Le avisamos por aquí cuando esté listo.',
    '',
    contacto,
  ].join('\n')

  const listo = [
    `Hola ${orden.cliente}, ya está listo su trabajo: ${orden.productoRecibido} (orden ${orden.numeroOrden}).`,
    orden.garantia ? 'Es garantía: sin costo.' : debe,
    'Puede pasar a recogerlo.',
    '',
    contacto,
  ].join('\n')

  const enlaceComprobante = enlaceWhatsApp(orden.telefonoCliente, comprobante)
  const enlaceListo = enlaceWhatsApp(orden.telefonoCliente, listo)
  if (!enlaceComprobante || !enlaceListo) {
    return <p className="text-sm text-sub">Para avisarle por WhatsApp, corrige el teléfono del cliente en Clientes.</p>
  }

  const clase = 'inline-flex min-h-12 items-center justify-center gap-2 rounded-lg px-4 text-base font-semibold'
  return (
    <div className="flex flex-col gap-2 border-t border-hair pt-3">
      <span className="font-medium">WhatsApp a {orden.cliente}</span>
      {orden.estado === 'LISTO' && (
        <a href={enlaceListo} target="_blank" rel="noopener noreferrer" className={`${clase} bg-steel-dark text-white`}>
          <MessageCircle size={20} aria-hidden /> Avisar que ya está listo
        </a>
      )}
      <a href={enlaceComprobante} target="_blank" rel="noopener noreferrer" className={`${clase} border-2 border-line bg-card`}>
        <MessageCircle size={20} aria-hidden /> Enviar comprobante
      </a>
    </div>
  )
}
