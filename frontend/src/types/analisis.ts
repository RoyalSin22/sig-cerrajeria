/** Lo cargado como ventas de antes del sistema (null en desde/hasta si no hay nada). */
export interface ResumenVentasAnteriores {
  filas: number
  total: number
  desde: string | null
  hasta: string | null
}

export interface LineaVentaAnterior {
  fila: number
  fecha: string
  producto: string
  /** Coincidió con un producto del inventario (por código o nombre). */
  enlazada: boolean
  cantidad: number
  precioUnitario: number
  metodoPago: string | null
}

export interface ResultadoVentasAnteriores {
  filasLeidas: number
  cargadas: number
  enlazadas: number
  total: number
  desde: string | null
  hasta: string | null
  muestra: LineaVentaAnterior[]
  errores: { fila: number; mensaje: string }[]
  columnasIgnoradas: string[]
}
