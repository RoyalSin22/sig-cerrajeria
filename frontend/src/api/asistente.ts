import { axiosClient } from './axiosClient'
import type { AsistenteMensajeRequest, AsistenteMensajeResponse } from '../types/asistente'

export const asistenteApi = {
  /** false si no hay clave de Anthropic: la app no muestra el chat. */
  disponible: () =>
    axiosClient.get<{ disponible: boolean }>('/asistente/estado').then(r => r.data.disponible).catch(() => false),
  enviarMensaje: (body: AsistenteMensajeRequest) =>
    axiosClient.post<AsistenteMensajeResponse>('/asistente/mensaje', body).then(r => r.data),
}
