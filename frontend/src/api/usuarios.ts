import { axiosClient } from './axiosClient'
import type { UsuarioResponse, UsuarioCreateRequest } from '../types/usuarios'

export const usuariosApi = {
  listar: () => axiosClient.get<UsuarioResponse[]>('/usuarios').then(r => r.data),
  crear: (body: UsuarioCreateRequest) => axiosClient.post<UsuarioResponse>('/usuarios', body).then(r => r.data),
  cambiarEstado: (id: number, activo: boolean) =>
    axiosClient.patch<UsuarioResponse>(`/usuarios/${id}/estado`, { activo }).then(r => r.data),
  /** Cambia la contrasena del usuario en sesion; cierra sus demas sesiones. */
  cambiarMiPassword: (passwordActual: string, passwordNueva: string) =>
    axiosClient.patch<void>('/usuarios/me/password', { passwordActual, passwordNueva }).then(() => undefined),
  /** Solo ADMIN: restablece la contrasena de otro usuario (ej. la olvido). */
  restablecerPassword: (id: number, passwordNueva: string) =>
    axiosClient.patch<void>(`/usuarios/${id}/password`, { passwordNueva }).then(() => undefined),
}
