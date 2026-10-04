import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Minus, Plus, Trash2 } from 'lucide-react'
import { productosApi } from '../api/productos'
import { ventasApi } from '../api/ventas'
import { extraerMensajeError } from '../api/axiosClient'
import { SearchInput } from '../components/ui/SearchInput'
import { EmptyState } from '../components/ui/EmptyState'
import { Button } from '../components/ui/Button'
import { Chip } from '../components/ui/Chip'
import { useCaja } from '../context/useCaja'
import { useToast } from '../context/useToast'
import { formatearSoles } from '../utils/format'
import { ETIQUETA_METODO_PAGO, METODOS_EN_USO, type MetodoPago, type MetodoPagoSimple } from '../types/ventas'
import type { ProductoResponse } from '../types/productos'

interface ItemCarrito {
  producto: ProductoResponse
  cantidad: number
}

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

  function agregarAlCarrito(producto: ProductoResponse) {
    setCarrito((prev) => {
      const existente = prev.find((i) => i.producto.id === producto.id)
      if (existente) {
        return prev.map((i) =>
          i.producto.id === producto.id ? { ...i, cantidad: i.cantidad + 1 } : i,
        )
      }
      return [...prev, { producto, cantidad: 1 }]
    })
  }

  function cambiarCantidad(productoId: number, delta: number) {
    setCarrito((prev) =>
      prev
        .map((i) => (i.producto.id === productoId ? { ...i, cantidad: i.cantidad + delta } : i))
        .filter((i) => i.cantidad > 0),
    )
  }

  function quitarDelCarrito(productoId: number) {
    setCarrito((prev) => prev.filter((i) => i.producto.id !== productoId))
  }

  const total = carrito.reduce((acc, i) => acc + i.producto.precioVenta * i.cantidad, 0)

  const sumaDesglose = METODOS_EN_USO.reduce((acc, m) => acc + (Number(desglose[m]) || 0), 0)
  const desgloseValido = Math.abs(sumaDesglose - total) < 0.005 && sumaDesglose > 0
  const puedeCobrar = metodoPago !== 'MIXTO' || desgloseValido

  async function handleCobrar() {
    setError(null)
    setProcesando(true)
    try {
      await ventasApi.crear({
        metodoPago,
        items: carrito.map((i) => ({ productoId: i.producto.id, cantidad: i.cantidad })),
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
      <div className="mx-auto max-w-sm py-10 text-center">
        <p className="mb-4 text-sub">No tienes una caja abierta. Ábrela para poder registrar ventas.</p>
        <Button onClick={() => (window.location.href = '/caja')}>Ir a Caja</Button>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4 pb-28">
      <div className="flex justify-end">
        <button onClick={() => navigate('/venta/historial')} className="text-xs text-steel underline">
          Ver historial de ventas
        </button>
      </div>
      <SearchInput placeholder="Buscar producto por nombre o código…" onBuscar={handleBuscar} autoFocus />

      {resultados.length > 0 && (
        <div className="flex flex-col gap-2">
          {resultados.map((p) => (
            <button
              key={p.id}
              onClick={() => agregarAlCarrito(p)}
              className="panel-cepillado flex items-center justify-between rounded-lg p-3 text-left"
            >
              <div>
                <p className="text-sm font-medium">{p.nombre}</p>
                <p className="text-xs text-sub">{p.codigo} · Stock: {p.cantidadStock}</p>
              </div>
              <span className="font-heading font-semibold text-steel-dark">{formatearSoles(p.precioVenta)}</span>
            </button>
          ))}
        </div>
      )}

      <div className="flex flex-col gap-2">
        <span className="font-heading text-lg font-semibold">Carrito</span>
        {carrito.length === 0 ? (
          <EmptyState mensaje="Busca un producto arriba para agregarlo a la venta." />
        ) : (
          carrito.map((item) => (
            <div key={item.producto.id} className="flex items-center justify-between gap-2 border-b border-hair pb-2">
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium">{item.producto.nombre}</p>
                <p className="text-xs text-sub">{formatearSoles(item.producto.precioVenta)} c/u</p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => cambiarCantidad(item.producto.id, -1)}
                  className="flex h-9 w-9 items-center justify-center rounded-md border border-line hover:bg-hair"
                  aria-label="Restar"
                >
                  <Minus size={16} />
                </button>
                <span className="w-6 text-center text-sm font-medium">{item.cantidad}</span>
                <button
                  onClick={() => cambiarCantidad(item.producto.id, 1)}
                  className="flex h-9 w-9 items-center justify-center rounded-md border border-line hover:bg-hair"
                  aria-label="Sumar"
                >
                  <Plus size={16} />
                </button>
                <button
                  onClick={() => quitarDelCarrito(item.producto.id)}
                  className="flex h-9 w-9 items-center justify-center rounded-md text-danger hover:bg-hair"
                  aria-label="Quitar"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Zona fija inferior (thumb zone): total + metodo de pago + cobrar */}
      {carrito.length > 0 && (
        <div className="fixed inset-x-0 bottom-0 z-20 border-t border-hair bg-paper p-4 shadow-lg md:sticky md:bottom-4 md:rounded-lg md:border">
          <div className="mx-auto flex max-w-xl flex-col gap-3">
            <div className="flex items-center justify-between">
              <span className="text-sm text-sub">Total</span>
              <span className="font-heading text-3xl font-bold text-steel-dark">{formatearSoles(total)}</span>
            </div>
            <div className="flex gap-2 overflow-x-auto">
              {METODOS.map((m) => (
                <Chip key={m} activo={metodoPago === m} onClick={() => setMetodoPago(m)}>
                  {ETIQUETA_METODO_PAGO[m]}
                </Chip>
              ))}
            </div>

            {metodoPago === 'MIXTO' && (
              <div className="flex flex-col gap-2 rounded-md border border-line p-3">
                <span className="text-xs text-sub">Desglosa el pago por método (debe sumar el total)</span>
                <div className="grid grid-cols-2 gap-2">
                  {METODOS_EN_USO.map((m) => (
                    // Etiqueta visible: el placeholder desaparece al escribir y no se sabria que campo es cual
                    <label key={m} className="flex flex-col gap-1 text-xs text-sub">
                      {ETIQUETA_METODO_PAGO[m]} (S/)
                      <input
                        type="number" step="0.01" min="0"
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

            {error && <p className="text-sm text-danger">{error}</p>}
            <Button onClick={handleCobrar} disabled={procesando || !puedeCobrar} className="w-full text-base">
              {procesando ? 'Procesando…' : 'Cobrar'}
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
