import { useCallback, useState, type ReactNode } from 'react'
import { ToastContext, type TipoToast } from './useToast'

interface ToastItem {
  id: number
  mensaje: string
  tipo: TipoToast
}

const estilosPorTipo: Record<TipoToast, string> = {
  success: 'bg-steel-dark',
  error: 'bg-danger',
  info: 'bg-ink',
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])

  const notificar = useCallback((mensaje: string, tipo: TipoToast = 'success') => {
    const id = Date.now()
    setToasts((prev) => [...prev, { id, mensaje, tipo }])
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, tipo === 'error' ? 7000 : 4500) // tiempo para leerlo con calma
  }, [])

  return (
    <ToastContext.Provider value={{ notificar }}>
      {children}
      <div
        role="status"
        aria-live="polite"
        className="fixed left-1/2 top-16 z-[60] flex w-full max-w-md -translate-x-1/2 flex-col gap-2 px-4"
      >
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`rounded-lg px-5 py-4 text-base font-semibold text-white shadow-lg animate-in fade-in slide-in-from-top-2 ${estilosPorTipo[t.tipo]}`}
          >
            {t.mensaje}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
