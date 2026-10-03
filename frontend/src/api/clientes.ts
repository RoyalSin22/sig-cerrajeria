import { axiosClient } from './axiosClient'
import type { Pagina } from '../types/pagina'
import type { ClienteResponse, ClienteRequest } from '../types/clientes'
import type { VentaResponse } from '../types/ventas'
import type { OrdenTrabajoResponse } from '../types/ordenes'

export const clientesApi = {
  listar: (page: number) =>
    axiosClient.get<Pagina<ClienteResponse>>('/clientes', { params: { page, size: 20 } }).then(r => r.data),
  buscar: (q: string) => axiosClient.get<ClienteResponse[]>('/clientes/buscar', { params: { q } }).then(r => r.data),
  obtener: (id: number) => axiosClient.get<ClienteResponse>(`/clientes/${id}`).then(r => r.data),
  crear: (body: ClienteRequest) => axiosClient.post<ClienteResponse>('/clientes', body).then(r => r.data),
  actualizar: (id: number, body: ClienteRequest) =>
    axiosClient.put<ClienteResponse>(`/clientes/${id}`, body).then(r => r.data),
  historialCompras: (id: number) => axiosClient.get<VentaResponse[]>(`/clientes/${id}/compras`).then(r => r.data),
  historialOrdenes: (id: number) => axiosClient.get<OrdenTrabajoResponse[]>(`/clientes/${id}/ordenes`).then(r => r.data),
}
