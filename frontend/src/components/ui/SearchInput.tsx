import { Search, X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'

interface Props {
  placeholder?: string
  onBuscar: (termino: string) => void
  debounceMs?: number
  autoFocus?: boolean
}

/**
 * Buscador con debounce de 300ms (patron usado en toda la app: RF-18, RF-46).
 * type="search" muestra la tecla "Buscar" en el teclado del celular, y la "x"
 * grande borra lo escrito de un toque.
 */
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
      <Search size={22} aria-hidden className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sub" />
      <input
        type="search"
        enterKeyHint="search"
        autoComplete="off"
        autoCorrect="off"
        spellCheck={false}
        aria-label={placeholder.replace(/…$/, '')}
        autoFocus={autoFocus}
        value={valor}
        onChange={(e) => setValor(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-lg border bg-card py-2.5 pl-11 pr-12 outline-none"
      />
      {valor && (
        <button
          type="button"
          onClick={() => setValor('')}
          aria-label="Borrar búsqueda"
          className="absolute right-1 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-md text-sub hover:bg-hair"
        >
          <X size={22} />
        </button>
      )}
    </div>
  )
}
