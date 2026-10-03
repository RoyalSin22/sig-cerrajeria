import { axiosClient } from './axiosClient'
import type { ResultadoBusquedaResponse } from '../types/busqueda'

export const busquedaApi = {
  buscar: (q: string) => axiosClient.get<ResultadoBusquedaResponse>('/buscar', { params: { q } }).then(r => r.data),
}
