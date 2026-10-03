import { axiosClient } from './axiosClient'
import type { UsuarioResponse, UsuarioCreateRequest } from '../types/usuarios'

export const usuariosApi = {
  listar: () => axiosClient.get<UsuarioResponse[]>('/usuarios').then(r => r.data),
  crear: (body: UsuarioCreateRequest) => axiosClient.post<UsuarioResponse>('/usuarios', body).then(r => r.data),
  cambiarEstado: (id: number, activo: boolean) =>
    axiosClient.patch<UsuarioResponse>(`/usuarios/${id}/estado`, { activo }).then(r => r.data),
}
