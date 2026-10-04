import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import type { Rol } from '../types/auth'

interface Props {
  children: ReactNode
  soloRol?: Rol
}

/** Protege rutas que exigen sesion iniciada; opcionalmente, un rol especifico (ADMIN). */
export function ProtectedRoute({ children, soloRol }: Props) {
  const { usuario, cargandoSesion } = useAuth()

  if (cargandoSesion) {
    return (
      <div className="flex h-screen items-center justify-center text-sub">
        Cargando…
      </div>
    )
  }

  if (!usuario) {
    return <Navigate to="/login" replace />
  }

  if (soloRol && usuario.role !== soloRol) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-2 text-center px-6">
        <p className="font-heading text-2xl font-semibold">Acceso restringido</p>
        <p className="text-sub">Esta sección es solo para administradores.</p>
      </div>
    )
  }

  return <>{children}</>
}
