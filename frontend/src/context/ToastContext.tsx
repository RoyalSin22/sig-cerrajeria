import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'

type TipoToast = 'success' | 'error' | 'info'

interface ToastItem {
  id: number
  mensaje: string
  tipo: TipoToast
}

interface ToastContextValue {
  notificar: (mensaje: string, tipo?: TipoToast) => void
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined)

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

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) throw new Error('useToast debe usarse dentro de <ToastProvider>')
  return context
}
