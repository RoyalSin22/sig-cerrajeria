import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
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
import type { ProveedorResponse } from '../types/compras'
import type { ProductoResponse } from '../types/productos'

export function Compras() {
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const { notificar } = useToast()
  const { cajaAbierta, refrescarCaja } = useCaja()

  const [proveedores, setProveedores] = useState<ProveedorResponse[]>([])
  const [proveedorId, setProveedorId] = useState<number | ''>('')
  const [nuevoProveedor, setNuevoProveedor] = useState('')

  const [productoEncontrado, setProductoEncontrado] = useState<ProductoResponse[]>([])
  const [productoId, setProductoId] = useState<number | null>(null)
  const [productoNombre, setProductoNombre] = useState('')

  const [cantidad, setCantidad] = useState('')
  const [precioUnitario, setPrecioUnitario] = useState('')
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
    if (termino.trim().length >= 2) {
      setProductoEncontrado(await productosApi.buscar(termino.trim()))
    } else {
      setProductoEncontrado([])
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!proveedorId || !productoId) {
      setError('Selecciona proveedor y producto')
      return
    }
    setError(null)
    setGuardando(true)
    try {
      const compra = await comprasApi.crearCompra({
        proveedorId,
        productoId,
        cantidad: Number(cantidad),
        precioUnitario: Number(precioUnitario),
        pagarDeCaja,
      })
      notificar(`Compra registrada: +${compra.cantidad} unidades de ${compra.producto}`)
      setCantidad(''); setPrecioUnitario(''); setProductoId(null); setProductoNombre(''); setPagarDeCaja(false)
      if (pagarDeCaja) await refrescarCaja()
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="mx-auto flex max-w-lg flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-2xl font-semibold">Registrar compra</h1>
        <div className="flex gap-2">
          <Button variant="ghost" onClick={() => navigate('/compras/historial')}>Historial</Button>
          <Button variant="ghost" onClick={() => navigate('/proveedores')}>Ver proveedores</Button>
        </div>
      </div>

      {usuario?.role !== 'ROLE_ADMIN' ? (
        // El backend solo acepta compras del administrador (POST /compras)
        <Card className="text-sm text-sub">
          Solo el administrador registra compras. Puedes ver las anteriores en el historial.
        </Card>
      ) : (
      <form onSubmit={handleSubmit} className="panel-cepillado flex flex-col gap-4 rounded-lg p-5">
        <div>
          <label className="mb-1 block text-sm font-medium">Proveedor</label>
          <select
            value={proveedorId}
            onChange={(e) => setProveedorId(e.target.value ? Number(e.target.value) : '')}
            className="w-full rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel"
          >
            <option value="">Selecciona un proveedor…</option>
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
          <label className="mb-1 block text-sm font-medium">Producto</label>
          {productoId ? (
            <div className="flex items-center justify-between rounded-md bg-tint-sky p-2 text-sm">
              <span>{productoNombre}</span>
              <button type="button" onClick={() => setProductoId(null)} className="text-steel underline">Cambiar</button>
            </div>
          ) : (
            <>
              <SearchInput placeholder="Buscar producto por nombre o código…" onBuscar={buscarProducto} />
              {productoEncontrado.length > 0 && (
                <div className="mt-2 flex flex-col gap-1">
                  {productoEncontrado.map((p) => (
                    <button key={p.id} type="button"
                      onClick={() => { setProductoId(p.id); setProductoNombre(p.nombre) }}
                      className="text-left">
                      <Card className="flex items-center justify-between py-2">
                        <span className="text-sm">{p.nombre}</span>
                        <span className="text-xs text-sub">Stock actual: {p.cantidadStock}</span>
                      </Card>
                    </button>
                  ))}
                </div>
              )}
            </>
          )}
        </div>

        <div className="flex gap-3">
          <div className="flex-1">
            <label className="mb-1 block text-sm font-medium">Cantidad</label>
            <input required type="number" min="1" value={cantidad} onChange={(e) => setCantidad(e.target.value)}
              className="w-full rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel" />
          </div>
          <div className="flex-1">
            <label className="mb-1 block text-sm font-medium">Precio unitario (S/)</label>
            <input required type="number" step="0.01" min="0" value={precioUnitario} onChange={(e) => setPrecioUnitario(e.target.value)}
              className="w-full rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel" />
          </div>
        </div>

        {cantidad && precioUnitario && (
          <p className="text-sm text-sub">
            Total: <span className="font-medium text-ink">{formatearSoles(Number(cantidad) * Number(precioUnitario))}</span>
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

        {error && <p className="text-sm text-danger">{error}</p>}
        <Button type="submit" disabled={guardando}>{guardando ? 'Registrando…' : 'Registrar compra'}</Button>
      </form>
      )}
    </div>
  )
}
