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

/**
 * Metodos que se ofrecen al cobrar. La cerrajeria no cobra con tarjeta: el
 * backend lo sigue aceptando (y ETIQUETA_METODO_PAGO lo muestra en ventas
 * antiguas), pero no aparece en los selectores.
 */
export const METODOS_EN_USO: MetodoPagoSimple[] = ['EFECTIVO', 'YAPE', 'PLIN']

/** Un producto del inventario, o un servicio rapido sin producto (descripcion + precio). */
export interface ItemVentaRequest {
  productoId?: number
  descripcion?: string
  precio?: number
  cantidad: number
  /** Cobra el precio de duplicado de la llave en vez del de la llave sola. */
  duplicado?: boolean
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
  /** null en un servicio rapido */
  productoId: number | null
  nombreProducto: string
  cantidad: number
  precioUnitario: number
  subtotal: number
  duplicado: boolean
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
