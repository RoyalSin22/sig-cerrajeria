import { axiosClient } from './axiosClient'
import type {
  OrdenTrabajoCreateRequest, OrdenTrabajoResponse, CambiarEstadoOrdenRequest, RegistrarPagoOrdenRequest, CancelarOrdenRequest,
  GarantiaRequest, MaterialOrdenResponse,
} from '../types/ordenes'

export const ordenesApi = {
  listar: (estado?: string) =>
    axiosClient.get<OrdenTrabajoResponse[]>('/ordenes', { params: estado ? { estado } : {} }).then(r => r.data),
  obtener: (id: number) => axiosClient.get<OrdenTrabajoResponse>(`/ordenes/${id}`).then(r => r.data),
  crear: (body: OrdenTrabajoCreateRequest) =>
    axiosClient.post<OrdenTrabajoResponse>('/ordenes', body).then(r => r.data),
  cambiarEstado: (id: number, body: CambiarEstadoOrdenRequest) =>
    axiosClient.patch<OrdenTrabajoResponse>(`/ordenes/${id}/estado`, body).then(r => r.data),
  registrarPago: (id: number, body: RegistrarPagoOrdenRequest) =>
    axiosClient.post<OrdenTrabajoResponse>(`/ordenes/${id}/pagos`, body).then(r => r.data),
  entregar: (id: number, body?: RegistrarPagoOrdenRequest) =>
    axiosClient.post<OrdenTrabajoResponse>(`/ordenes/${id}/entregar`, body).then(r => r.data),
  /** Orden sin cobro sobre una orden entregada (el cliente vuelve porque algo fallo). */
  garantia: (id: number, body: GarantiaRequest) =>
    axiosClient.post<OrdenTrabajoResponse>(`/ordenes/${id}/garantia`, body).then(r => r.data),
  materiales: (id: number) =>
    axiosClient.get<MaterialOrdenResponse[]>(`/ordenes/${id}/materiales`).then(r => r.data),
  /** El material sale del stock por el Kardex. */
  usarMaterial: (id: number, productoId: number, cantidad: number) =>
    axiosClient.post<MaterialOrdenResponse[]>(`/ordenes/${id}/materiales`, { productoId, cantidad }).then(r => r.data),
  cancelar: (id: number, body: CancelarOrdenRequest) =>
    axiosClient.post<OrdenTrabajoResponse>(`/ordenes/${id}/cancelar`, body).then(r => r.data),
}
