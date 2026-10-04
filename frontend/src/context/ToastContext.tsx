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
    }, 3000)
  }, [])

  return (
    <ToastContext.Provider value={{ notificar }}>
      {children}
      <div className="fixed bottom-20 left-1/2 z-50 flex w-full max-w-sm -translate-x-1/2 flex-col gap-2 px-4 md:bottom-6">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`rounded-lg px-4 py-3 text-sm text-white shadow-lg animate-in fade-in slide-in-from-bottom-2 ${estilosPorTipo[t.tipo]}`}
          >
            {t.mensaje}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
