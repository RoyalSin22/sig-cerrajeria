import { Search } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'

interface Props {
  placeholder?: string
  onBuscar: (termino: string) => void
  debounceMs?: number
  autoFocus?: boolean
}

/** Buscador con debounce de 300ms (patron usado en toda la app: RF-18, RF-46). */
export function SearchInput({ placeholder = 'Buscar…', onBuscar, debounceMs = 300, autoFocus }: Props) {
  const [valor, setValor] = useState('')
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current)
    timeoutRef.current = setTimeout(() => onBuscar(valor), debounceMs)
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [valor])

  return (
    <div className="relative">
      <Search size={18} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sub" />
      <input
        autoFocus={autoFocus}
        value={valor}
        onChange={(e) => setValor(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-md border border-line bg-paper py-2.5 pl-10 pr-3 text-sm outline-none focus:border-steel"
      />
    </div>
  )
}
