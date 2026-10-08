import { useEffect, useState, type ReactNode } from 'react'
import { AuthContext } from './useAuth'
import { axiosClient, setAccessToken } from '../api/axiosClient'
import type { LoginRequest, LoginResponse, UsuarioAutenticado } from '../types/auth'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioAutenticado | null>(null)
  const [cargandoSesion, setCargandoSesion] = useState(true)

  // Al cargar la app, intenta recuperar sesion usando la cookie de refresh
  // (por ejemplo, tras recargar la pagina con F5).
  useEffect(() => {
    axiosClient
      .post<LoginResponse>('/auth/refresh')
      .then((res) => {
        setAccessToken(res.data.accessToken)
        setUsuario({ username: res.data.username, nombre: res.data.nombre, role: res.data.role })
      })
      .catch(() => {
        setAccessToken(null)
        setUsuario(null)
      })
      .finally(() => setCargandoSesion(false))
  }, [])

  useEffect(() => {
    function alExpirarSesion() {
      setAccessToken(null)
      setUsuario(null)
    }
    window.addEventListener('sig:sesion-expirada', alExpirarSesion)
    return () => window.removeEventListener('sig:sesion-expirada', alExpirarSesion)
  }, [])

  async function login(credenciales: LoginRequest) {
    const res = await axiosClient.post<LoginResponse>('/auth/login', credenciales)
    setAccessToken(res.data.accessToken)
    setUsuario({ username: res.data.username, nombre: res.data.nombre, role: res.data.role })
  }

  async function logout() {
    try {
      await axiosClient.post('/auth/logout')
    } finally {
      setAccessToken(null)
      setUsuario(null)
    }
  }

  return (
    <AuthContext.Provider value={{ usuario, cargandoSesion, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
