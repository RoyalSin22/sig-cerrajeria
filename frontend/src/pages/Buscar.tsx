import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { busquedaApi } from '../api/busqueda'
import { SearchInput } from '../components/ui/SearchInput'
import { EmptyState } from '../components/ui/EmptyState'
import { Card } from '../components/ui/Card'
import { formatearSoles } from '../utils/format'
import { ETIQUETA_ESTADO_ORDEN } from '../types/ordenes'
import type { ResultadoBusquedaResponse } from '../types/busqueda'

/**
 * RF-46: buscador global instantaneo, cruzando productos, clientes y ordenes.
 * En vez de una pestaña propia, se accede desde el icono de lupa de la
 * barra superior (Fase 7 §4).
 */
export function Buscar() {
  const navigate = useNavigate()
  const [resultado, setResultado] = useState<ResultadoBusquedaResponse | null>(null)
  const [buscado, setBuscado] = useState(false)

  async function handleBuscar(termino: string) {
    if (termino.trim().length < 2) {
      setResultado(null)
      setBuscado(false)
      return
    }
    setResultado(await busquedaApi.buscar(termino.trim()))
    setBuscado(true)
  }

  const sinResultados =
    buscado && resultado &&
    resultado.productos.length === 0 && resultado.clientes.length === 0 && resultado.ordenes.length === 0

  return (
    <div className="flex flex-col gap-5">
      <h1 className="font-heading text-2xl font-semibold">Buscar</h1>
      <SearchInput placeholder="Producto, cliente u orden…" onBuscar={handleBuscar} autoFocus />

      {!buscado && <EmptyState mensaje="Escribe al menos 2 caracteres para buscar en todo el sistema." />}
      {sinResultados && <EmptyState mensaje="No se encontraron coincidencias." />}

      {resultado && resultado.productos.length > 0 && (
        <section>
          <span className="font-heading mb-2 block text-sm font-semibold text-sub">PRODUCTOS</span>
          <div className="flex flex-col gap-2">
            {resultado.productos.map((p) => (
              <button key={p.id} onClick={() => navigate('/inventario')} className="text-left">
                <Card className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium">{p.nombre}</p>
                    <p className="text-xs text-sub">{p.codigo}</p>
                  </div>
                  <span className="font-heading font-semibold text-steel-dark">{formatearSoles(p.precioVenta)}</span>
                </Card>
              </button>
            ))}
          </div>
        </section>
      )}

      {resultado && resultado.clientes.length > 0 && (
        <section>
          <span className="font-heading mb-2 block text-sm font-semibold text-sub">CLIENTES</span>
          <div className="flex flex-col gap-2">
            {resultado.clientes.map((c) => (
              <button key={c.id} onClick={() => navigate('/clientes')} className="text-left">
                <Card>
                  <p className="text-sm font-medium">{c.nombre}</p>
                  <p className="text-xs text-sub">{c.telefono}</p>
                </Card>
              </button>
            ))}
          </div>
        </section>
      )}

      {resultado && resultado.ordenes.length > 0 && (
        <section>
          <span className="font-heading mb-2 block text-sm font-semibold text-sub">ÓRDENES</span>
          <div className="flex flex-col gap-2">
            {resultado.ordenes.map((o) => (
              <button key={o.id} onClick={() => navigate('/taller')} className="text-left">
                <Card className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium">{o.numeroOrden} — {o.cliente}</p>
                    <p className="text-xs text-sub">{o.productoRecibido}</p>
                  </div>
                  <span className="text-xs text-sub">{ETIQUETA_ESTADO_ORDEN[o.estado]}</span>
                </Card>
              </button>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
