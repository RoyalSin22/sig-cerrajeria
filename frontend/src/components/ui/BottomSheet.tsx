import { X } from 'lucide-react'
import type { ReactNode } from 'react'

interface Props {
  abierto: boolean
  onCerrar: () => void
  titulo: string
  children: ReactNode
}

/** Ficha de detalle como hoja inferior (Fase 7 §5.0): producto, orden, servicio. */
export function BottomSheet({ abierto, onCerrar, titulo, children }: Props) {
  if (!abierto) return null

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center md:items-center">
      <div className="absolute inset-0 bg-ink/40" onClick={onCerrar} />
      <div className="panel-cepillado relative z-10 max-h-[90vh] w-full overflow-y-auto rounded-t-2xl p-5 md:max-w-lg md:rounded-2xl">
        <div className="mb-4 flex items-center justify-between">
          <span className="font-heading text-xl font-semibold">{titulo}</span>
          <button onClick={onCerrar} aria-label="Cerrar" className="rounded-md p-1 hover:bg-hair">
            <X size={20} />
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
