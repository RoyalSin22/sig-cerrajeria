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
      <div className="absolute inset-0 bg-ink/50" onClick={onCerrar} />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        className="panel-cepillado relative z-10 max-h-[92vh] w-full overflow-y-auto rounded-t-2xl p-5 md:max-w-xl md:rounded-2xl"
      >
        <div className="mb-4 flex items-start justify-between gap-3">
          <span className="font-heading text-2xl font-bold leading-tight">{titulo}</span>
          {/* Con texto, no solo una "x": se entiende que cierra */}
          <button
            onClick={onCerrar}
            className="inline-flex min-h-12 shrink-0 items-center gap-1 rounded-lg border-2 border-line px-3 font-semibold hover:bg-hair"
          >
            <X size={22} aria-hidden /> Cerrar
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
