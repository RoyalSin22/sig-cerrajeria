// Refleja com.cerrajeria.sig.domicilio.dto.*

export type EstadoServicio = 'PROGRAMADO' | 'EN_CAMINO' | 'REALIZADO' | 'CANCELADO'

export const ETIQUETA_ESTADO_SERVICIO: Record<EstadoServicio, string> = {
  PROGRAMADO: 'Programado',
  EN_CAMINO: 'En camino',
  REALIZADO: 'Realizado',
  CANCELADO: 'Cancelado',
}

export interface ServicioDomicilioRequest {
  clienteId: number
  direccion: string
  telefono: string
  fecha: string // yyyy-MM-dd
  hora: string // HH:mm
  tipoServicio: string
  observaciones?: string
}

export interface ServicioDomicilioResponse {
  id: number
  cliente: string
  direccion: string
  telefono: string
  fecha: string
  hora: string
  tipoServicio: string
  observaciones: string | null
  estado: EstadoServicio
}
