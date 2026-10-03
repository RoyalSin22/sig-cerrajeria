interface Props {
  activo?: boolean
  onClick?: () => void
  children: React.ReactNode
}

/** Fase 7 §5.0: chips de filtro horizontales, mas rapidos de tocar que un <select>. */
export function Chip({ activo, onClick, children }: Props) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`shrink-0 rounded-full border px-3.5 py-1.5 text-sm font-medium transition-colors min-h-9 ${
        activo
          ? 'border-steel-dark bg-steel-dark text-white'
          : 'border-line text-sub hover:border-steel hover:text-steel'
      }`}
    >
      {children}
    </button>
  )
}
