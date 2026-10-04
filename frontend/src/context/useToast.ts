import { createContext, useContext } from 'react'

export type TipoToast = 'success' | 'error' | 'info'

export interface ToastContextValue {
  notificar: (mensaje: string, tipo?: TipoToast) => void
}

export const ToastContext = createContext<ToastContextValue | undefined>(undefined)

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) throw new Error('useToast debe usarse dentro de <ToastProvider>')
  return context
}
