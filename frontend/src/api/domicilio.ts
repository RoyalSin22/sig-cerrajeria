import { axiosClient } from './axiosClient'
import type {
  ServicioDomicilioRequest, ServicioDomicilioResponse,
} from '../types/domicilio'

export const domicilioApi = {
  agenda: (desde: string, hasta: string) =>
    axiosClient.get<ServicioDomicilioResponse[]>('/servicios-domicilio/agenda', { params: { desde, hasta } }).then(r => r.data),
  crear: (body: ServicioDomicilioRequest) =>
    axiosClient.post<ServicioDomicilioResponse>('/servicios-domicilio', body).then(r => r.data),
  cambiarEstado: (id: number, nuevoEstado: string) =>
    axiosClient.patch<ServicioDomicilioResponse>(`/servicios-domicilio/${id}/estado`, { nuevoEstado }).then(r => r.data),
}
