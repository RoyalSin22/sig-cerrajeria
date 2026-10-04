import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { CajaContext } from './useCaja'
import { axiosClient } from '../api/axiosClient'
import { useAuth } from './useAuth'
import type { CajaDiariaResponse } from '../types/caja'

export function CajaProvider({ children }: { children: ReactNode }) {
  const { usuario } = useAuth()
  const [caja, setCaja] = useState<CajaDiariaResponse | null>(null)
  const [cargando, setCargando] = useState(true)

  const refrescarCaja = useCallback(async () => {
    setCargando(true)
    try {
      const res = await axiosClient.get<CajaDiariaResponse>('/caja/abierta')
      setCaja(res.data)
    } catch {
      setCaja(null) // 404/409: no hay caja abierta (regla de negocio 1)
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    if (usuario) {
      refrescarCaja()
    } else {
      setCaja(null)
      setCargando(false)
    }
  }, [usuario, refrescarCaja])

  return (
    <CajaContext.Provider value={{ caja, cargando, cajaAbierta: caja !== null, refrescarCaja }}>
      {children}
    </CajaContext.Provider>
  )
}
