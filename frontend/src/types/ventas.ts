// Refleja com.cerrajeria.sig.ventas.dto.* y MetodoPago

export type MetodoPago = 'EFECTIVO' | 'YAPE' | 'PLIN' | 'TARJETA' | 'MIXTO'
/** Las lineas de un desglose nunca son MIXTO (no tendria sentido). */
export type MetodoPagoSimple = Exclude<MetodoPago, 'MIXTO'>

export const ETIQUETA_METODO_PAGO: Record<MetodoPago, string> = {
  EFECTIVO: 'Efectivo',
  YAPE: 'Yape',
  PLIN: 'Plin',
  TARJETA: 'Tarjeta',
  MIXTO: 'Mixto',
}

export const METODOS_SIMPLES: MetodoPagoSimple[] = ['EFECTIVO', 'YAPE', 'PLIN', 'TARJETA']

export interface ItemVentaRequest {
  productoId: number
  cantidad: number
}

export interface DesglosePagoRequest {
  metodoPago: MetodoPagoSimple
  monto: number
}

export interface DesglosePagoResponse {
  metodoPago: MetodoPagoSimple
  monto: number
}

export interface VentaCreateRequest {
  clienteId?: number
  metodoPago: MetodoPago
  items: ItemVentaRequest[]
  /** Obligatorio y debe sumar el total cuando metodoPago = 'MIXTO'. */
  desglosePago?: DesglosePagoRequest[]
}

export interface DetalleVentaResponse {
  productoId: number
  nombreProducto: string
  cantidad: number
  precioUnitario: number
  subtotal: number
}

export interface VentaResponse {
  id: number
  cliente: string | null
  usuario: string
  fechaHora: string
  montoTotal: number
  metodoPago: MetodoPago
  detalles: DetalleVentaResponse[]
  anulada: boolean
  desglosePago: DesglosePagoResponse[]
}
