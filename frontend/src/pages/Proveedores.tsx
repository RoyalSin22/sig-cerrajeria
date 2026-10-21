import { useEffect, useState, type FormEvent } from 'react'
import { comprasApi } from '../api/compras'
import { extraerMensajeError } from '../api/axiosClient'
import { Card } from '../components/ui/Card'
import { Button } from '../components/ui/Button'
import { Campo } from '../components/ui/Campo'
import { EmptyState } from '../components/ui/EmptyState'
import { useToast } from '../context/useToast'
import type { ProveedorResponse } from '../types/compras'

export function Proveedores() {
  const { notificar } = useToast()
  const [proveedores, setProveedores] = useState<ProveedorResponse[]>([])
  const [mostrarAlta, setMostrarAlta] = useState(false)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    comprasApi.listarProveedores().then(setProveedores).finally(() => setCargando(false))
  }, [])

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-3xl font-bold">Proveedores</h1>
        <Button onClick={() => setMostrarAlta((v) => !v)}>{mostrarAlta ? 'Cancelar' : 'Nuevo proveedor'}</Button>
      </div>

      {mostrarAlta && (
        <FormularioProveedor
          onCreado={(p) => {
            notificar('Proveedor registrado')
            setProveedores((prev) => [...prev, p])
            setMostrarAlta(false)
          }}
        />
      )}

      {cargando ? (
        <p className="text-sm text-sub">Cargando…</p>
      ) : proveedores.length === 0 ? (
        <EmptyState mensaje="Todavía no hay proveedores registrados." />
      ) : (
        <div className="flex flex-col gap-2">
          {proveedores.map((p) => (
            <Card key={p.id}>
              <p className="text-sm font-medium">{p.nombre}</p>
              {(p.telefono || p.direccion) && (
                <p className="text-xs text-sub">{[p.telefono, p.direccion].filter(Boolean).join(' · ')}</p>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}

function FormularioProveedor({ onCreado }: { onCreado: (p: ProveedorResponse) => void }) {
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
      const proveedor = await comprasApi.crearProveedor({
        nombre, telefono: telefono || undefined, direccion: direccion || undefined,
      })
      setNombre(''); setTelefono(''); setDireccion('')
      onCreado(proveedor)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="panel-cepillado flex flex-col gap-3 rounded-lg p-4">
      <Campo etiqueta="Nombre"><input required value={nombre} onChange={(e) => setNombre(e.target.value)}
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" /></Campo>
      <Campo etiqueta="Teléfono" opcional><input type="tel" inputMode="tel" autoComplete="off" value={telefono} onChange={(e) => setTelefono(e.target.value)}
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" /></Campo>
      <Campo etiqueta="Dirección" opcional><input value={direccion} onChange={(e) => setDireccion(e.target.value)}
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" /></Campo>
      {error && <p className="text-base font-semibold text-danger">{error}</p>}
      <Button type="submit" tamano="lg" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar'}</Button>
    </form>
  )
}
