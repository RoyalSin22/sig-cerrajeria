interface Props {
  activo?: boolean
  onClick?: () => void
  children: React.ReactNode
}

/** Chips de filtro o atajo: grandes (44 px) y con el elegido bien marcado. */
export function Chip({ activo, onClick, children }: Props) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={activo}
      className={`min-h-11 shrink-0 rounded-full border-2 px-4 py-1.5 text-base font-semibold transition-colors ${
        activo
          ? 'border-steel-dark bg-steel-dark text-white'
          : 'border-line bg-card text-ink hover:border-steel hover:text-steel'
      }`}
    >
      {children}
    </button>
  )
}
