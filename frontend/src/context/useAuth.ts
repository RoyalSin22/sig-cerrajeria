import { createContext, useContext } from 'react'
import type { LoginRequest, UsuarioAutenticado } from '../types/auth'

export interface AuthContextValue {
  usuario: UsuarioAutenticado | null
  cargandoSesion: boolean
  login: (credenciales: LoginRequest) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return context
}
