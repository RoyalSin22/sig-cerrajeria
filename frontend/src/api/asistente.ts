import { axiosClient } from './axiosClient'
import type { AsistenteMensajeRequest, AsistenteMensajeResponse } from '../types/asistente'

export const asistenteApi = {
  enviarMensaje: (body: AsistenteMensajeRequest) =>
    axiosClient.post<AsistenteMensajeResponse>('/asistente/mensaje', body).then(r => r.data),
}
