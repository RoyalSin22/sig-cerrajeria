// Refleja com.cerrajeria.sig.asistente.dto.*

export interface TurnoChat {
  rol: 'user' | 'assistant'
  texto: string
}

export interface AsistenteMensajeRequest {
  mensaje: string
  historial: TurnoChat[]
}

export interface AsistenteMensajeResponse {
  respuesta: string
}
