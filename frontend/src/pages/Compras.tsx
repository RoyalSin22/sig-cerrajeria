import { useEffect, useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ClipboardList, Trash2 } from 'lucide-react'
import { comprasApi } from '../api/compras'
import { productosApi } from '../api/productos'
import { extraerMensajeError } from '../api/axiosClient'
import { SearchInput } from '../components/ui/SearchInput'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { useToast } from '../context/useToast'
import { useCaja } from '../context/useCaja'
import { useAuth } from '../context/useAuth'
import { formatearSoles } from '../utils/format'
import { guardarLista, leerLista } from '../utils/listaCompras'
import type { ProveedorResponse } from '../types/compras'
import type { ProductoResponse } from '../types/productos'

/** Una linea de la compra; cantidad y precio como texto para poder dejarlos vacios mientras se escriben. */
interface Linea {
  productoId: number
  nombre: string
  stock: number
  cantidad: string
  precioUnitario: string
}

/**
 * Todo lo que se trajo de un distribuidor en una sola vuelta: se registra junto
 * (todo o nada) y cada producto entra al stock por el Kardex.
 */
export function Compras() {
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { notificar } = useToast()
  const { cajaAbierta, refrescarCaja } = useCaja()

  const [proveedores, setProveedores] = useState<ProveedorResponse[]>([])
  const [proveedorId, setProveedorId] = useState<number | ''>('')
  const [nuevoProveedor, setNuevoProveedor] = useState('')

  const [encontrados, setEncontrados] = useState<ProductoResponse[]>([])
  const [lineas, setLineas] = useState<Linea[]>(() =>
    (location.state as { desdeLista?: boolean } | null)?.desdeLista
      ? leerLista().filter((i) => i.cantidad > 0).map((i) => ({
          productoId: i.productoId,
          nombre: i.nombre,
          stock: i.stock,
          cantidad: String(i.cantidad),
          precioUnitario: i.precioCompra > 0 ? String(i.precioCompra) : '',
        }))
      : [],
  )
  const [pagarDeCaja, setPagarDeCaja] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    comprasApi.listarProveedores().then(setProveedores)
  }, [])

  async function crearProveedorRapido() {
    if (!nuevoProveedor.trim()) return
    const p = await comprasApi.crearProveedor({ nombre: nuevoProveedor.trim() })
    setProveedores((prev) => [...prev, p])
    setProveedorId(p.id)
    setNuevoProveedor('')
  }

  async function buscarProducto(termino: string) {
    setEncontrados(termino.trim().length >= 2 ? await productosApi.buscar(termino.trim()) : [])
  }

  function agregarLinea(p: ProductoResponse) {
    if (!lineas.some((l) => l.productoId === p.id)) {
      setLineas((prev) => [...prev, {
        productoId: p.id,
        nombre: p.nombre,
        stock: p.cantidadStock,
        cantidad: '',
        precioUnitario: p.precioCompra > 0 ? String(p.precioCompra) : '',
      }])
    }
    setEncontrados([])
  }

  function cambiarLinea(productoId: number, campo: 'cantidad' | 'precioUnitario', valor: string) {
    setLineas((prev) => prev.map((l) => (l.productoId === productoId ? { ...l, [campo]: valor } : l)))
  }

  const total = lineas.reduce((suma, l) => suma + (Number(l.cantidad) || 0) * (Number(l.precioUnitario) || 0), 0)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!proveedorId) {
      setError('Selecciona el distribuidor')
      return
    }
    if (lineas.length === 0) {
      setError('Agrega al menos un producto')
      return
    }
    const incompleta = lineas.find((l) => !(Number(l.cantidad) > 0) || !(Number(l.precioUnitario) > 0))
    if (incompleta) {
      setError(`Falta la cantidad o el precio de ${incompleta.nombre}`)
      return
    }
    setError(null)
    setGuardando(true)
    try {
      const compras = await comprasApi.crearLote({
        proveedorId,
        items: lineas.map((l) => ({
          productoId: l.productoId,
          cantidad: Math.trunc(Number(l.cantidad)),
          precioUnitario: Number(l.precioUnitario),
        })),
        pagarDeCaja,
      })
      notificar(compras.length === 1
        ? `Compra registrada: +${compras[0].cantidad} de ${compras[0].producto}`
        : `Compra registrada: ${compras.length} productos`)
      // Lo comprado sale de la lista de compras
      const comprados = new Set(lineas.map((l) => l.productoId))
      guardarLista(leerLista().filter((i) => !comprados.has(i.productoId)))
      setLineas([])
      setPagarDeCaja(false)
      if (pagarDeCaja) await refrescarCaja()
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  const campo = 'w-full rounded-md border border-line bg-paper px-2 py-2 text-sm outline-none focus:border-steel'

  return (
    <div className="mx-auto flex max-w-lg flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="font-heading text-3xl font-bold">Registrar compra</h1>
        <div className="flex gap-2">
          <Button variant="ghost" onClick={() => navigate('/compras/historial')}>Historial</Button>
          <Button variant="ghost" onClick={() => navigate('/proveedores')}>Proveedores</Button>
        </div>
      </div>

      <Button variant="secondary" className="flex items-center justify-center gap-2" onClick={() => navigate('/compras/lista')}>
        <ClipboardList size={18} aria-hidden /> Lista de compras
      </Button>

      {usuario?.role !== 'ROLE_ADMIN' ? (
        // El backend solo acepta compras del administrador (POST /compras)
        <Card className="text-sm text-sub">
          Solo el administrador registra compras. Puedes ver las anteriores en el historial.
        </Card>
      ) : (
      <form onSubmit={handleSubmit} className="panel-cepillado flex flex-col gap-4 rounded-lg p-5">
        <div>
          <label className="mb-1 block text-sm font-medium">Distribuidor</label>
          <select
            value={proveedorId}
            onChange={(e) => setProveedorId(e.target.value ? Number(e.target.value) : '')}
            className="w-full rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel"
          >
            <option value="">Selecciona un distribuidor…</option>
            {proveedores.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
          </select>
          <div className="mt-2 flex gap-2">
            <input value={nuevoProveedor} onChange={(e) => setNuevoProveedor(e.target.value)}
              placeholder="O registra uno nuevo…"
              className="flex-1 rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" />
            <Button type="button" variant="secondary" onClick={crearProveedorRapido}>Agregar</Button>
          </div>
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium">Productos</label>
          <SearchInput placeholder="Buscar producto por nombre o código…" onBuscar={buscarProducto} />
          {encontrados.length > 0 && (
            <div className="mt-2 flex flex-col gap-1">
              {encontrados.map((p) => (
                <button key={p.id} type="button" onClick={() => agregarLinea(p)} className="text-left">
                  <Card className="flex items-center justify-between py-2">
                    <span className="text-sm">{p.nombre}</span>
                    <span className="text-xs text-sub">Stock actual: {p.cantidadStock}</span>
                  </Card>
                </button>
              ))}
            </div>
          )}
        </div>

        {lineas.length > 0 && (
          <ul className="flex flex-col gap-2">
            {lineas.map((l) => (
              <li key={l.productoId} className="rounded-md border border-hair bg-paper p-3">
                <div className="mb-2 flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="text-sm font-medium">{l.nombre}</p>
                    <p className="text-xs text-sub">Stock actual: {l.stock}</p>
                  </div>
                  <button
                    type="button" aria-label={`Quitar ${l.nombre}`}
                    onClick={() => setLineas((prev) => prev.filter((x) => x.productoId !== l.productoId))}
                    className="rounded-md p-1.5 text-sub hover:bg-hair hover:text-danger"
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
                <div className="flex gap-2">
                  <label className="flex-1 text-xs text-sub">
                    Cantidad
                    <input type="number" min="1" inputMode="numeric" value={l.cantidad}
                      onChange={(e) => cambiarLinea(l.productoId, 'cantidad', e.target.value)} className={campo} />
                  </label>
                  <label className="flex-1 text-xs text-sub">
                    Precio unitario (S/)
                    <input type="number" step="0.01" min="0" inputMode="decimal" value={l.precioUnitario}
                      onChange={(e) => cambiarLinea(l.productoId, 'precioUnitario', e.target.value)} className={campo} />
                  </label>
                </div>
              </li>
            ))}
          </ul>
        )}

        {total > 0 && (
          <p className="text-sm text-sub">
            Total: <span className="font-medium text-ink">{formatearSoles(total)}</span>
          </p>
        )}

        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox" checked={pagarDeCaja}
            disabled={!cajaAbierta}
            onChange={(e) => setPagarDeCaja(e.target.checked)}
          />
          Pagar de la caja activa {!cajaAbierta && '(no tienes caja abierta)'}
        </label>

        {error && <p className="text-base font-semibold text-danger">{error}</p>}
        <Button type="submit" tamano="lg" disabled={guardando}>{guardando ? 'Registrando…' : 'Registrar compra'}</Button>
      </form>
      )}
    </div>
  )
}
