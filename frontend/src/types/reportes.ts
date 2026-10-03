// Refleja com.cerrajeria.sig.reportes.*

export interface VentaPorMetodoPago {
  metodoPago: string
  total: number
}

export interface ReporteVentasResponse {
  totalVentas: number
  cantidadTransacciones: number
  porMetodoPago: VentaPorMetodoPago[]
}

export interface ProductoMasVendidoResponse {
  productoId: number
  nombre: string
  cantidadVendida: number
  totalVendido: number
}

export interface ItemInventario {
  codigo: string
  nombre: string
  tipoProducto: string
  cantidadStock: number
  precioVenta: number
  valorLinea: number
  stockBajo: boolean
}

export interface ReporteInventarioResponse {
  totalProductosActivos: number
  totalUnidades: number
  valorTotalInventario: number
  items: ItemInventario[]
}
