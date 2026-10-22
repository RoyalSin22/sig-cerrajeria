import { axiosClient } from './axiosClient'

/** Refleja negocio.dto.NegocioDto */
export interface Negocio {
  nombre: string
  telefono: string
  direccion: string
}

export const negocioApi = {
  obtener: () => axiosClient.get<Negocio>('/negocio').then(r => r.data),
  guardar: (datos: Negocio) => axiosClient.put<Negocio>('/negocio', datos).then(r => r.data),
}
