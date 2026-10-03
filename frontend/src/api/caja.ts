import { axiosClient } from './axiosClient'
import type { Pagina } from '../types/pagina'
import type { CajaDiariaResponse } from '../types/caja'

export const cajaApi = {
  historial: (page: number) =>
    axiosClient.get<Pagina<CajaDiariaResponse>>('/caja/historial', { params: { page, size: 20 } }).then(r => r.data),
}
