import { createContext, useContext } from 'react'
import type { CajaDiariaResponse } from '../types/caja'

export interface CajaContextValue {
  caja: CajaDiariaResponse | null
  cargando: boolean
  cajaAbierta: boolean
  refrescarCaja: () => Promise<void>
}

export const CajaContext = createContext<CajaContextValue | undefined>(undefined)

export function useCaja() {
  const context = useContext(CajaContext)
  if (!context) throw new Error('useCaja debe usarse dentro de <CajaProvider>')
  return context
}
