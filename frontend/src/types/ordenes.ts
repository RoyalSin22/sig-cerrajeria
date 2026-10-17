// Refleja com.cerrajeria.sig.ordenes.dto.* y sus enums

export type EstadoOrden = 'RECIBIDO' | 'EN_REPARACION' | 'ESPERANDO_REPUESTO' | 'LISTO' | 'ENTREGADO' | 'CANCELADO'

/** Flujo normal de una orden, en orden. CANCELADO queda fuera: se llega con "Cancelar orden". */
export const ESTADOS_ORDEN: EstadoOrden[] = ['RECIBIDO', 'EN_REPARACION', 'ESPERANDO_REPUESTO', 'LISTO', 'ENTREGADO']

export const ETIQUETA_ESTADO_ORDEN: Record<EstadoOrden, string> = {
  RECIBIDO: 'Recibido',
  EN_REPARACION: 'En reparación',
  ESPERANDO_REPUESTO: 'Esperando repuesto',
  LISTO: 'Listo',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
}

export interface OrdenTrabajoCreateRequest {
  clienteId: number
  productoRecibido: string
  problema: string
  diagnostico?: string
  precioTotal: number
  adelanto?: number
  metodoPagoAdelanto?: string
  fechaPrometida?: string
  observaciones?: string
}

export interface OrdenTrabajoResponse {
  id: number
  numeroOrden: string
  cliente: string
  productoRecibido: string
  problema: string
  diagnostico: string | null
  precioTotal: number
  montoPagado: number
  saldoPendiente: number
  estado: EstadoOrden
  fechaIngreso: string
  fechaPrometida: string | null
  fechaEntrega: string | null
  observaciones: string | null
  motivoCancelacion: string | null
}

export interface CancelarOrdenRequest {
  motivo: string
  devolverAdelanto: boolean
  metodoDevolucion?: string
}

export interface CambiarEstadoOrdenRequest {
  nuevoEstado: EstadoOrden
}

export interface RegistrarPagoOrdenRequest {
  monto: number
  metodoPago: string
}
