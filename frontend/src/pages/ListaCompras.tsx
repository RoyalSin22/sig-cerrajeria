import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertTriangle, Copy, Share2, Trash2 } from 'lucide-react'
import { productosApi } from '../api/productos'
import { SearchInput } from '../components/ui/SearchInput'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { useToast } from '../context/useToast'
import { useAuth } from '../context/useAuth'
import { formatearFecha } from '../utils/format'
import { hoyISO } from '../utils/fecha'
import {
  guardarLista, itemDesdeProducto, leerLista, textoLista, type ItemListaCompras,
} from '../utils/listaCompras'
import type { ProductoResponse } from '../types/productos'

/**
 * Lista para ir a comprar donde los distribuidores: se arma con lo que esta en
 * stock bajo y lo que se agregue a mano, se manda por WhatsApp y, al volver,
 * pasa a "Registrar compra" con las cantidades ya puestas.
 */
export function ListaCompras() {
  const navigate = useNavigate()
  const { notificar } = useToast()
  const { usuario } = useAuth()
  const [items, setItems] = useState<ItemListaCompras[]>(leerLista)
  const [stockBajo, setStockBajo] = useState<ProductoResponse[]>([])
  const [encontrados, setEncontrados] = useState<ProductoResponse[]>([])

  useEffect(() => {
    productosApi.stockBajo().then(setStockBajo)
  }, [])

  function cambiar(nuevos: ItemListaCompras[]) {
    setItems(nuevos)
    guardarLista(nuevos)
  }

  const faltanDeStockBajo = stockBajo.filter((p) => !items.some((i) => i.productoId === p.id))

  function agregar(productos: ProductoResponse[]) {
    const nuevos = productos.filter((p) => !items.some((i) => i.productoId === p.id)).map(itemDesdeProducto)
    cambiar([...items, ...nuevos])
    setEncontrados([])
  }

  function fijarCantidad(productoId: number, texto: string) {
    const n = parseInt(texto, 10)
    cambiar(items.map((i) => (i.productoId === productoId ? { ...i, cantidad: Number.isNaN(n) ? 0 : n } : i)))
  }

  async function buscar(termino: string) {
    setEncontrados(termino.trim().length >= 2 ? await productosApi.buscar(termino.trim()) : [])
  }

  const validos = items.filter((i) => i.cantidad > 0)
  const texto = textoLista(validos, formatearFecha(hoyISO()))

  async function compartir() {
    if (navigator.share) {
      try {
        await navigator.share({ text: texto })
        return
      } catch {
        // Cancelado o no soportado: se abre WhatsApp directamente
      }
    }
    window.open(`https://wa.me/?text=${encodeURIComponent(texto)}`, '_blank', 'noopener')
  }

  async function copiar() {
    try {
      await navigator.clipboard.writeText(texto)
      notificar('Lista copiada')
    } catch {
      notificar('No se pudo copiar', 'error')
    }
  }

  return (
    <div className="mx-auto flex max-w-lg flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-3xl font-bold">Lista de compras</h1>
        <Button variant="ghost" onClick={() => navigate('/compras')}>Volver</Button>
      </div>

      {faltanDeStockBajo.length > 0 && (
        <Card className="flex items-center justify-between gap-3 text-sm">
          <span className="flex items-center gap-2">
            <AlertTriangle size={16} className="shrink-0 text-warning" aria-hidden />
            {faltanDeStockBajo.length === 1
              ? '1 producto en stock bajo no está en la lista'
              : `${faltanDeStockBajo.length} productos en stock bajo no están en la lista`}
          </span>
          <Button variant="secondary" onClick={() => agregar(faltanDeStockBajo)}>Agregar</Button>
        </Card>
      )}

      <div>
        <SearchInput placeholder="Agregar otro producto por nombre o código…" onBuscar={buscar} />
        {encontrados.length > 0 && (
          <div className="mt-2 flex flex-col gap-1">
            {encontrados.map((p) => (
              <button key={p.id} type="button" onClick={() => agregar([p])} className="text-left">
                <Card className="flex items-center justify-between py-2">
                  <span className="text-sm">{p.nombre}</span>
                  <span className="text-xs text-sub">Hay {p.cantidadStock}</span>
                </Card>
              </button>
            ))}
          </div>
        )}
      </div>

      {items.length === 0 ? (
        <EmptyState mensaje="La lista está vacía. Agrega lo que está en stock bajo o busca un producto." />
      ) : (
        <ul className="flex flex-col gap-2">
          {items.map((i) => (
            <li key={i.productoId}>
              <Card className="flex items-center gap-3 py-2">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{i.nombre}</p>
                  <p className="text-xs text-sub">{i.codigo} · hay {i.stock}</p>
                </div>
                <input
                  type="number" min="1" inputMode="numeric"
                  aria-label={`Cantidad a comprar de ${i.nombre}`}
                  value={i.cantidad === 0 ? '' : i.cantidad}
                  onChange={(e) => fijarCantidad(i.productoId, e.target.value)}
                  className="w-20 rounded-md border border-line bg-paper px-2 py-2 text-right text-sm outline-none focus:border-steel"
                />
                <button
                  type="button" aria-label={`Quitar ${i.nombre}`}
                  onClick={() => cambiar(items.filter((x) => x.productoId !== i.productoId))}
                  className="rounded-md p-2 text-sub hover:bg-hair hover:text-danger"
                >
                  <Trash2 size={18} />
                </button>
              </Card>
            </li>
          ))}
        </ul>
      )}

      {validos.length > 0 && (
        <div className="flex flex-col gap-2">
          <div className="flex gap-2">
            <Button className="flex flex-1 items-center justify-center gap-2" onClick={compartir}>
              <Share2 size={16} aria-hidden /> Enviar por WhatsApp
            </Button>
            <Button variant="secondary" className="flex items-center gap-2" onClick={copiar}>
              <Copy size={16} aria-hidden /> Copiar
            </Button>
          </div>
          {usuario?.role === 'ROLE_ADMIN' && (
            <Button variant="secondary" onClick={() => navigate('/compras', { state: { desdeLista: true } })}>
              Ya compré: registrar la compra
            </Button>
          )}
          <button type="button" onClick={() => cambiar([])} className="self-center text-sm text-sub underline">
            Vaciar lista
          </button>
        </div>
      )}
    </div>
  )
}
