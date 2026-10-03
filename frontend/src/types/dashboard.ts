// Refleja com.cerrajeria.sig.dashboard.DashboardResponse

export interface MovimientoReciente {
  descripcion: string
  usuario: string
  fechaHora: string
}

export interface DashboardResponse {
  ventasHoy: number
  ventasMes: number
  trabajosPendientes: number
  trabajosListos: number
  serviciosDomicilioProximos: number
  productosStockBajo: number
  alertasStockBajo: string[]
  ultimosMovimientos: MovimientoReciente[]
}
