// Refleja com.cerrajeria.sig.reportes.*

export interface VentaPorMetodoPago {
  metodoPago: string
  total: number
}

export interface ReporteVentasResponse {
  totalVentas: number
  cantidadTransacciones: number
  porMetodoPago: VentaPorMetodoPago[]
  /** Adelantos y liquidaciones del taller, menos devoluciones */
  ingresosTaller: number
  ingresosDomicilio: number
  serviciosDomicilioRealizados: number
  /** Ventas + taller + domicilio */
  ingresosTotales: number
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

/** RF-43: servicios realizados en el rango */
export interface GrupoServicios {
  nombre: string
  cantidad: number
  total: number
}

export interface ReporteServiciosResponse {
  domicilio: {
    realizados: number
    cancelados: number
    cobrado: number
    porTipo: GrupoServicios[]
    porTecnico: GrupoServicios[]
  }
  taller: {
    recibidas: number
    entregadas: number
    /** Abiertas hoy, no depende del rango */
    abiertas: number
    ingresosNetos: number
  }
}

/** RF-44: lo que dejó cada cliente en el rango */
export interface MovimientoCliente {
  cantidad: number
  total: number
}

export interface ReporteClientesResponse {
  clientesConMovimiento: number
  /** Ventas de mostrador sin cliente asociado */
  ventasSinCliente: number
  ranking: {
    clienteId: number
    nombre: string
    ventas: MovimientoCliente
    taller: MovimientoCliente
    domicilio: MovimientoCliente
    total: number
  }[]
}
