import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { KeyRound, Minus, Plus, Trash2, Wallet, Wrench } from 'lucide-react'
import { productosApi } from '../api/productos'
import { ventasApi } from '../api/ventas'
import { extraerMensajeError } from '../api/axiosClient'
import { SearchInput } from '../components/ui/SearchInput'
import { EmptyState } from '../components/ui/EmptyState'
import { Button } from '../components/ui/Button'
import { Campo } from '../components/ui/Campo'
import { BottomSheet } from '../components/ui/BottomSheet'
import { useCaja } from '../context/useCaja'
import { useToast } from '../context/useToast'
import { formatearSoles } from '../utils/format'
import { ETIQUETA_METODO_PAGO, METODOS_EN_USO, type MetodoPago, type MetodoPagoSimple } from '../types/ventas'
import type { ProductoResponse } from '../types/productos'

/** Un producto del inventario, o un servicio rapido (sin producto: abrir un candado, afilar). */
interface ItemCarrito {
  producto?: ProductoResponse
  servicio?: { descripcion: string; precio: number }
  cantidad: number
  /** Duplicado (material + corte) o la llave/producto solo: la misma llave puede estar dos veces. */
  duplicado: boolean
}

const clave = (i: ItemCarrito) =>
  i.servicio ? `servicio-${i.servicio.descripcion}-${i.servicio.precio}` : `${i.producto!.id}-${i.duplicado ? 'dup' : 'sola'}`
const precioUnitario = (i: ItemCarrito) =>
  i.servicio ? i.servicio.precio
    : i.duplicado ? i.producto!.precioDuplicado ?? i.producto!.precioVenta : i.producto!.precioVenta
const nombre = (i: ItemCarrito) => (i.servicio ? i.servicio.descripcion : i.producto!.nombre)

/** Los servicios de siempre, para no escribirlos: solo se pone el precio. */
const SERVICIOS_COMUNES = [
  'Abrir candado', 'Abrir puerta', 'Cambio de combinación', 'Instalación de chapa', 'Reparación de licuadora', 'Afilar cuchilla',
]

const METODOS: MetodoPago[] = [...METODOS_EN_USO, 'MIXTO']

export function Venta() {
  const navigate = useNavigate()
  const { cajaAbierta, refrescarCaja } = useCaja()
  const { notificar } = useToast()

  const [resultados, setResultados] = useState<ProductoResponse[]>([])
  const [carrito, setCarrito] = useState<ItemCarrito[]>([])
  const [metodoPago, setMetodoPago] = useState<MetodoPago>('EFECTIVO')
  const [desglose, setDesglose] = useState<Record<MetodoPagoSimple, string>>({
    EFECTIVO: '', YAPE: '', PLIN: '', TARJETA: '',
  })
  const [procesando, setProcesando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Desde Inicio, "Duplicado de llave" llega con ?duplicado=1 y abre la hoja directo
  const [params, setParams] = useSearchParams()
  const [mostrarDuplicado, setMostrarDuplicado] = useState(params.get('duplicado') === '1')
  const [mostrarServicio, setMostrarServicio] = useState(false)
  // "Venta rapida": lo de todos los dias a un toque (como los Favoritos de los POS de mostrador)
  const [rapidos, setRapidos] = useState<ProductoResponse[]>([])

  useEffect(() => {
    productosApi.ventaRapida().then(setRapidos).catch(() => setRapidos([]))
  }, [])

  async function handleBuscar(termino: string) {
    if (termino.trim().length < 2) {
      setResultados([])
      return
    }
    try {
      setResultados(await productosApi.buscar(termino.trim()))
    } catch {
      setResultados([])
    }
  }

  function agregarAlCarrito(producto: ProductoResponse, cantidad = 1, duplicado = false) {
    agregarItem({ producto, cantidad, duplicado })
  }

  function agregarItem(nuevo: ItemCarrito) {
    const cantidad = nuevo.cantidad
    setCarrito((prev) => {
      const existente = prev.find((i) => clave(i) === clave(nuevo))
      if (existente) {
        return prev.map((i) => (i === existente ? { ...i, cantidad: i.cantidad + cantidad } : i))
      }
      return [...prev, nuevo]
    })
  }

  /** Cantidad escrita a mano en el carrito (vacio o 0 no quita el item: para eso esta el tacho). */
  function fijarCantidad(item: ItemCarrito, texto: string) {
    const n = Number.parseInt(texto, 10)
    if (Number.isNaN(n) || n < 1) return
    setCarrito((prev) => prev.map((i) => (clave(i) === clave(item) ? { ...i, cantidad: n } : i)))
  }

  function cambiarCantidad(item: ItemCarrito, delta: number) {
    setCarrito((prev) =>
      prev
        .map((i) => (clave(i) === clave(item) ? { ...i, cantidad: i.cantidad + delta } : i))
        .filter((i) => i.cantidad > 0),
    )
  }

  function quitarDelCarrito(item: ItemCarrito) {
    setCarrito((prev) => prev.filter((i) => clave(i) !== clave(item)))
  }

  const total = carrito.reduce((acc, i) => acc + precioUnitario(i) * i.cantidad, 0)

  const sumaDesglose = METODOS_EN_USO.reduce((acc, m) => acc + (Number(desglose[m]) || 0), 0)
  const desgloseValido = Math.abs(sumaDesglose - total) < 0.005 && sumaDesglose > 0
  const puedeCobrar = metodoPago !== 'MIXTO' || desgloseValido

  async function handleCobrar() {
    setError(null)
    setProcesando(true)
    try {
      await ventasApi.crear({
        metodoPago,
        items: carrito.map((i) => (i.servicio
          ? { descripcion: i.servicio.descripcion, precio: i.servicio.precio, cantidad: i.cantidad }
          : { productoId: i.producto!.id, cantidad: i.cantidad, duplicado: i.duplicado })),
        desglosePago: metodoPago === 'MIXTO'
          ? METODOS_EN_USO
              .filter((m) => Number(desglose[m]) > 0)
              .map((m) => ({ metodoPago: m, monto: Number(desglose[m]) }))
          : undefined,
      })
      notificar(`Venta registrada: ${formatearSoles(total)}`)
      setCarrito([])
      setResultados([])
      setDesglose({ EFECTIVO: '', YAPE: '', PLIN: '', TARJETA: '' })
      await refrescarCaja() // el ingreso ya impacto el saldo de caja
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setProcesando(false)
    }
  }

  // Regla de negocio 1: sin caja abierta, no se puede ni empezar a vender.
  if (!cajaAbierta) {
    return (
      <div className="mx-auto flex max-w-sm flex-col items-center gap-4 py-10 text-center">
        <Wallet size={48} className="text-warning" aria-hidden />
        <p className="text-lg">Para vender, primero abre la caja.</p>
        <Button tamano="lg" onClick={() => navigate('/caja')} className="w-full">Abrir la caja</Button>
      </div>
    )
  }

  return (
    // Con productos en el carrito, abajo queda fija la zona de cobro: el espacio extra evita que tape el carrito
    <div className={`flex flex-col gap-4 ${carrito.length > 0 ? 'pb-[26rem] md:pb-4' : ''}`}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="font-heading text-3xl font-bold">Vender</h1>
        <Button variant="secondary" onClick={() => navigate('/venta/historial')}>Ventas de hoy</Button>
      </div>

      {/* Lo mas comun en el mostrador: botones grandes, un toque */}
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-[2fr_1fr]">
        <Button tamano="lg" onClick={() => setMostrarDuplicado(true)} className="min-h-16 w-full text-xl">
          <KeyRound size={26} aria-hidden /> Duplicado de llave
        </Button>
        <Button tamano="lg" variant="secondary" onClick={() => setMostrarServicio(true)} className="min-h-16 w-full text-xl">
          <Wrench size={24} aria-hidden /> Servicio
        </Button>
      </div>

      {rapidos.length > 0 && (
        <section aria-labelledby="titulo-rapida" className="flex flex-col gap-2">
          <h2 id="titulo-rapida" className="font-heading text-xl font-bold">
            Venta rápida <span className="text-base font-normal text-sub">· un toque y se agrega</span>
          </h2>
          <div className="grid grid-cols-2 gap-2 md:grid-cols-4">
            {rapidos.map((p) => {
              // Una llave con precio de duplicado se vende casi siempre como duplicado
              const duplicado = p.precioDuplicado != null
              const enCarrito = carrito.find((i) => i.producto?.id === p.id && i.duplicado === duplicado)?.cantidad ?? 0
              return (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => agregarAlCarrito(p, 1, duplicado)}
                  className={`flex min-h-20 flex-col items-start justify-between gap-1 rounded-xl border-2 p-3 text-left ${
                    enCarrito > 0 ? 'border-steel-dark bg-tint-sky' : 'border-hair bg-card hover:border-steel'
                  }`}
                >
                  <span className="font-semibold leading-tight">
                    {duplicado && <span className="text-steel-dark">Duplicado · </span>}
                    {p.nombre}
                  </span>
                  <span className="flex w-full items-baseline justify-between gap-1">
                    <b className="font-heading text-lg text-steel-dark">
                      {formatearSoles(duplicado ? p.precioDuplicado! : p.precioVenta)}
                    </b>
                    {enCarrito > 0 && <span className="text-sm font-bold text-ink">× {enCarrito}</span>}
                  </span>
                </button>
              )
            })}
          </div>
        </section>
      )}

      <SearchInput placeholder="Buscar otro producto…" onBuscar={handleBuscar} />

      {resultados.length > 0 && (
        <div className="flex flex-col gap-2">
          {resultados.map((p) =>
            p.precioDuplicado != null ? (
              // Una llave casi siempre se cobra como duplicado: es la opcion principal, la llave sola va aparte
              <div key={p.id} className="panel-cepillado flex flex-col gap-3 rounded-lg p-4">
                <div>
                  <p className="text-lg font-semibold">{p.nombre}</p>
                  <p className="text-sm text-sub">{p.codigo} · Quedan {p.cantidadStock}</p>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <Button onClick={() => agregarAlCarrito(p, 1, true)}>
                    Duplicado {formatearSoles(p.precioDuplicado)}
                  </Button>
                  <Button variant="secondary" onClick={() => agregarAlCarrito(p)}>
                    Sola {formatearSoles(p.precioVenta)}
                  </Button>
                </div>
              </div>
            ) : (
              <button
                key={p.id}
                onClick={() => agregarAlCarrito(p)}
                className="panel-cepillado flex min-h-16 items-center justify-between gap-3 rounded-lg p-4 text-left"
              >
                <div className="min-w-0">
                  <p className="text-lg font-semibold">{p.nombre}</p>
                  <p className="text-sm text-sub">{p.codigo} · Quedan {p.cantidadStock}</p>
                </div>
                <span className="shrink-0 font-heading text-xl font-bold text-steel-dark">{formatearSoles(p.precioVenta)}</span>
              </button>
            ),
          )}
        </div>
      )}

      <div className="flex flex-col gap-2">
        <span className="font-heading text-2xl font-bold">Lo que se lleva</span>
        {carrito.length === 0 ? (
          <EmptyState mensaje="Toca un botón de arriba o busca un producto." />
        ) : (
          carrito.map((item) => (
            <div key={clave(item)} className="panel-cepillado flex flex-col gap-2 rounded-lg p-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="min-w-0 flex-1">
                <p className="text-lg font-semibold">
                  {item.duplicado && <span className="text-steel-dark">Duplicado · </span>}
                  {item.servicio && <span className="text-steel-dark">Servicio · </span>}
                  {nombre(item)}
                </p>
                <p className="text-sm text-sub">
                  {formatearSoles(precioUnitario(item))} c/u · <b className="text-ink">{formatearSoles(precioUnitario(item) * item.cantidad)}</b>
                </p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => cambiarCantidad(item, -1)}
                  className="flex h-12 w-12 items-center justify-center rounded-lg border-2 border-line bg-card hover:bg-hair"
                  aria-label="Uno menos"
                >
                  <Minus size={22} />
                </button>
                <input
                  type="number"
                  inputMode="numeric"
                  min="1"
                  step="1"
                  aria-label={`Cantidad de ${nombre(item)}`}
                  value={item.cantidad}
                  onChange={(e) => fijarCantidad(item, e.target.value)}
                  className="w-16 rounded-lg border px-1 text-center text-xl font-bold outline-none"
                />
                <button
                  onClick={() => cambiarCantidad(item, 1)}
                  className="flex h-12 w-12 items-center justify-center rounded-lg border-2 border-line bg-card hover:bg-hair"
                  aria-label="Uno más"
                >
                  <Plus size={22} />
                </button>
                <button
                  onClick={() => quitarDelCarrito(item)}
                  className="ml-auto inline-flex min-h-12 items-center gap-1 rounded-lg px-3 font-semibold text-danger hover:bg-hair"
                >
                  <Trash2 size={20} aria-hidden /> Quitar
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Zona fija inferior (thumb zone): total + metodo de pago + cobrar */}
      {carrito.length > 0 && (
        // En el celular queda justo encima de la barra del menu (4.75rem)
        <div className="fixed inset-x-0 bottom-[4.75rem] z-20 border-t-2 border-hair bg-card p-4 shadow-[0_-4px_12px_rgba(0,0,0,0.08)] md:sticky md:bottom-4 md:rounded-xl md:border-2">
          <div className="mx-auto flex max-w-xl flex-col gap-3">
            <div className="flex items-baseline justify-between">
              <span className="text-lg font-semibold text-sub">Total</span>
              <span className="font-heading text-4xl font-bold text-steel-dark">{formatearSoles(total)}</span>
            </div>
            {/* ¿Cómo paga? Botones grandes en una fila, el elegido bien marcado */}
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4" role="radiogroup" aria-label="¿Cómo paga?">
              {METODOS.map((m) => (
                <button
                  key={m}
                  type="button"
                  role="radio"
                  aria-checked={metodoPago === m}
                  onClick={() => setMetodoPago(m)}
                  className={`min-h-12 rounded-lg border-2 px-1 text-base font-bold ${
                    metodoPago === m ? 'border-steel-dark bg-steel-dark text-white' : 'border-line bg-card text-ink'
                  }`}
                >
                  {ETIQUETA_METODO_PAGO[m]}
                </button>
              ))}
            </div>

            {metodoPago === 'MIXTO' && (
              <div className="flex flex-col gap-2 rounded-md border border-line p-3">
                <span className="text-xs text-sub">Desglosa el pago por método (debe sumar el total)</span>
                <div className="grid grid-cols-2 gap-2">
                  {METODOS_EN_USO.map((m) => (
                    // Etiqueta visible: el placeholder desaparece al escribir y no se sabria que campo es cual
                    <label key={m} className="flex flex-col gap-1 text-sm font-semibold">
                      {ETIQUETA_METODO_PAGO[m]} (S/)
                      <input
                        type="number" step="0.01" min="0" inputMode="decimal"
                        value={desglose[m]}
                        onChange={(e) => setDesglose((d) => ({ ...d, [m]: e.target.value }))}
                        placeholder="0.00"
                        className="rounded-md border border-line bg-paper px-3 py-2 text-sm text-ink outline-none focus:border-steel"
                      />
                    </label>
                  ))}
                </div>
                <p className={`text-xs ${desgloseValido ? 'text-success' : 'text-danger'}`}>
                  Desglosado: {formatearSoles(sumaDesglose)} de {formatearSoles(total)}
                </p>
              </div>
            )}

            {error && <p className="text-base font-semibold text-danger">{error}</p>}
            <Button tamano="lg" onClick={handleCobrar} disabled={procesando || !puedeCobrar} className="w-full text-xl">
              {procesando ? 'Cobrando…' : `Cobrar ${formatearSoles(total)}`}
            </Button>
          </div>
        </div>
      )}

      <ServicioRapido
        abierto={mostrarServicio}
        onCerrar={() => setMostrarServicio(false)}
        onAgregar={(descripcion, precio) => {
          agregarItem({ servicio: { descripcion, precio }, cantidad: 1, duplicado: false })
          setMostrarServicio(false)
        }}
      />

      <DuplicadoLlave
        abierto={mostrarDuplicado}
        onCerrar={() => {
          setMostrarDuplicado(false)
          if (params.has('duplicado')) setParams({}, { replace: true })
        }}
        onAgregar={(producto, cantidad) => {
          agregarAlCarrito(producto, cantidad, true)
          setMostrarDuplicado(false)
          if (params.has('duplicado')) setParams({}, { replace: true })
        }}
      />
    </div>
  )
}

/**
 * Duplicado de llave: se elige la llave y la cantidad, y va al carrito con su
 * precio de duplicado (material + corte), aparte del precio de la llave sola.
 * Descuenta la llave del stock por el Kardex y entra a la caja como cualquier
 * venta. No pide cliente: un duplicado se hace y se cobra en el momento. Solo
 * aparecen las llaves con precio de duplicado (se pone en su ficha).
 */
function DuplicadoLlave({
  abierto, onCerrar, onAgregar,
}: {
  abierto: boolean
  onCerrar: () => void
  onAgregar: (producto: ProductoResponse, cantidad: number) => void
}) {
  const [llaves, setLlaves] = useState<ProductoResponse[]>([])
  const [termino, setTermino] = useState('')
  const [elegida, setElegida] = useState<ProductoResponse | null>(null)
  const [cantidad, setCantidad] = useState('1')

  useEffect(() => {
    if (!abierto) return
    setElegida(null)
    setCantidad('1')
    setTermino('')
    productosApi.duplicables().then(setLlaves).catch(() => setLlaves([]))
  }, [abierto])

  // La lista completa ya esta cargada: buscar es filtrar
  const q = termino.trim().toLowerCase()
  const visibles = q.length === 0
    ? llaves
    : llaves.filter((p) => p.nombre.toLowerCase().includes(q) || p.codigo.toLowerCase().includes(q))
  const precio = elegida?.precioDuplicado ?? 0

  const n = Number.parseInt(cantidad, 10)
  const cantidadValida = Number.isInteger(n) && n >= 1
  const sinStock = elegida !== null && cantidadValida && n > elegida.cantidadStock

  return (
    <BottomSheet abierto={abierto} onCerrar={onCerrar} titulo="Duplicado de llave">
      {!elegida ? (
        <div className="flex flex-col gap-3">
          <SearchInput placeholder="Buscar llave (ej. KW1, Yale)…" onBuscar={setTermino} debounceMs={0} />
          {visibles.length === 0 ? (
            <p className="text-base text-sub">
              {termino
                ? 'No hay llaves con ese nombre.'
                : 'Ninguna llave tiene precio de duplicado. Ponlo en su ficha, en Inventario.'}
            </p>
          ) : (
            <div className="flex max-h-[55vh] flex-col gap-2 overflow-y-auto">
              {visibles.map((p) => (
                <button
                  key={p.id}
                  onClick={() => setElegida(p)}
                  className="panel-cepillado flex min-h-16 items-center justify-between gap-3 rounded-lg p-4 text-left hover:border-steel"
                >
                  <div className="min-w-0">
                    <p className="text-lg font-semibold">{p.nombre}</p>
                    <p className="text-sm text-sub">{p.codigo} · Quedan {p.cantidadStock}</p>
                  </div>
                  <span className="shrink-0 font-heading text-xl font-bold text-steel-dark">{formatearSoles(p.precioDuplicado)}</span>
                </button>
              ))}
            </div>
          )}
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between gap-2">
            <span className="text-xl font-bold">{elegida.nombre}</span>
            <Button variant="secondary" onClick={() => setElegida(null)}>Otra llave</Button>
          </div>
          {/* Un toque y listo: cada boton agrega esa cantidad al carrito */}
          <span className="text-lg font-semibold">¿Cuántas copias?</span>
          <div className="grid grid-cols-2 gap-2">
            {[1, 2, 3, 4].map((c) => (
              <button
                key={c}
                type="button"
                disabled={c > elegida.cantidadStock}
                onClick={() => onAgregar(elegida, c)}
                className="flex min-h-20 flex-col items-center justify-center rounded-xl border-2 border-steel-dark bg-tint-sky text-steel-dark disabled:opacity-40"
              >
                <span className="font-heading text-3xl font-bold leading-none">{c}</span>
                <span className="text-sm font-semibold">{formatearSoles(precio * c)}</span>
              </button>
            ))}
          </div>
          <label className="flex flex-col gap-1 text-base font-semibold">
            Otra cantidad
            <div className="flex gap-2">
              <input
                type="number" inputMode="numeric" min="1" step="1" value={cantidad}
                onChange={(e) => setCantidad(e.target.value)}
                className="w-28 rounded-lg border px-3 text-center text-xl font-bold outline-none"
              />
              <Button disabled={!cantidadValida || sinStock} onClick={() => onAgregar(elegida, n)} className="flex-1">
                Agregar {cantidadValida ? formatearSoles(precio * n) : ''}
              </Button>
            </div>
          </label>
          {sinStock && <p className="text-base font-semibold text-danger">Solo quedan {elegida.cantidadStock}.</p>}
        </div>
      )}
    </BottomSheet>
  )
}


/**
 * Servicio rapido: algo que se cobra en el mostrador sin producto del
 * inventario (abrir un candado, afilar una cuchilla). No mueve stock. Se toca
 * uno de los comunes (o se escribe) y se pone el precio.
 */
function ServicioRapido({
  abierto, onCerrar, onAgregar,
}: { abierto: boolean; onCerrar: () => void; onAgregar: (descripcion: string, precio: number) => void }) {
  const [descripcion, setDescripcion] = useState('')
  const [precio, setPrecio] = useState('')

  useEffect(() => {
    if (abierto) {
      setDescripcion('')
      setPrecio('')
    }
  }, [abierto])

  const monto = Number(precio)
  const valido = descripcion.trim().length > 0 && monto > 0

  return (
    <BottomSheet abierto={abierto} onCerrar={onCerrar} titulo="Cobrar un servicio">
      <div className="flex flex-col gap-4">
        <span className="text-lg font-semibold">¿Qué se hizo?</span>
        <div className="grid grid-cols-2 gap-2">
          {SERVICIOS_COMUNES.map((s) => (
            <button
              key={s}
              type="button"
              aria-pressed={descripcion === s}
              onClick={() => setDescripcion(s)}
              className={`min-h-14 rounded-xl border-2 px-2 text-base font-semibold ${
                descripcion === s ? 'border-steel-dark bg-tint-sky text-steel-dark' : 'border-hair bg-card'
              }`}
            >
              {s}
            </button>
          ))}
        </div>
        <Campo etiqueta="U otro">
          <input value={descripcion} onChange={(e) => setDescripcion(e.target.value)} maxLength={150}
            autoCapitalize="sentences" placeholder="Ej.: copia de llave de auto" className="rounded-lg border px-3 outline-none" />
        </Campo>
        <Campo etiqueta="Precio (S/)">
          <input type="number" inputMode="decimal" step="0.01" min="0" value={precio}
            onChange={(e) => setPrecio(e.target.value)} className="rounded-lg border px-3 text-2xl font-bold outline-none" />
        </Campo>
        <Button tamano="lg" disabled={!valido} onClick={() => onAgregar(descripcion.trim(), monto)}>
          Agregar {valido ? formatearSoles(monto) : ''}
        </Button>
      </div>
    </BottomSheet>
  )
}
