import { axiosClient } from './axiosClient'
import type { Pagina } from '../types/pagina'
import type { ProveedorRequest, ProveedorResponse, CompraCreateRequest, CompraResponse } from '../types/compras'

export const comprasApi = {
  listarProveedores: () => axiosClient.get<ProveedorResponse[]>('/proveedores').then(r => r.data),
  crearProveedor: (body: ProveedorRequest) =>
    axiosClient.post<ProveedorResponse>('/proveedores', body).then(r => r.data),
  crearCompra: (body: CompraCreateRequest) =>
    axiosClient.post<CompraResponse>('/compras', body).then(r => r.data),
  listarCompras: (page: number) =>
    axiosClient.get<Pagina<CompraResponse>>('/compras', { params: { page, size: 20 } }).then(r => r.data),
}
