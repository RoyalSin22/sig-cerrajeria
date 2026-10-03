import { axiosClient } from './axiosClient'
import type { Pagina } from '../types/pagina'
import type { VentaCreateRequest, VentaResponse } from '../types/ventas'

export const ventasApi = {
  crear: (body: VentaCreateRequest) => axiosClient.post<VentaResponse>('/ventas', body).then(r => r.data),
  listar: (page: number) =>
    axiosClient.get<Pagina<VentaResponse>>('/ventas', { params: { page, size: 20 } }).then(r => r.data),
  obtener: (id: number) => axiosClient.get<VentaResponse>(`/ventas/${id}`).then(r => r.data),
  anular: (id: number) => axiosClient.post<VentaResponse>(`/ventas/${id}/anular`).then(r => r.data),
}
