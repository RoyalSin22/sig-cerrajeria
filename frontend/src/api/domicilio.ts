import { axiosClient } from './axiosClient'
import type {
  ServicioDomicilioRequest, ServicioDomicilioResponse, RealizarServicioRequest,
} from '../types/domicilio'

export const domicilioApi = {
  agenda: (desde: string, hasta: string) =>
    axiosClient.get<ServicioDomicilioResponse[]>('/servicios-domicilio/agenda', { params: { desde, hasta } }).then(r => r.data),
  crear: (body: ServicioDomicilioRequest) =>
    axiosClient.post<ServicioDomicilioResponse>('/servicios-domicilio', body).then(r => r.data),
  cambiarEstado: (id: number, nuevoEstado: string) =>
    axiosClient.patch<ServicioDomicilioResponse>(`/servicios-domicilio/${id}/estado`, { nuevoEstado }).then(r => r.data),
  /** Realizado + cobro en la caja abierta de quien lo marca. */
  realizar: (id: number, body: RealizarServicioRequest) =>
    axiosClient.post<ServicioDomicilioResponse>(`/servicios-domicilio/${id}/realizar`, body).then(r => r.data),
}
