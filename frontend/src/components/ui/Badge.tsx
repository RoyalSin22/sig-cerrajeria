type Tono = 'neutral' | 'steel' | 'warning' | 'success' | 'dark'

const tonos: Record<Tono, string> = {
  neutral: 'bg-hair text-sub',
  steel: 'bg-tint-sky text-steel-dark',
  warning: 'bg-tint-peach text-warning',
  success: 'bg-tint-mint text-success',
  dark: 'bg-steel-dark text-white',
}

/** Fase 7 §3.3: colores de badge por estado de orden. */
export function Badge({ children, tono = 'neutral' }: { children: React.ReactNode; tono?: Tono }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium ${tonos[tono]}`}>
      {children}
    </span>
  )
}
