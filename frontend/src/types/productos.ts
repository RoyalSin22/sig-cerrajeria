// Refleja com.cerrajeria.sig.inventario.dto.* y TipoProducto

export type TipoProducto =
  | 'LLAVE_VIRGEN' | 'LLAVE_COLOR' | 'LLAVE_DISENO' | 'CHAPA' | 'CILINDRO'
  | 'CANDADO' | 'LLAVERO' | 'ARGOLLA' | 'ACCESORIO' | 'REPUESTO_LICUADORA'

/** Las llaves se pueden ofrecer como duplicado (con su propio precio). */
export function esLlave(tipo: TipoProducto): boolean {
  return tipo === 'LLAVE_VIRGEN' || tipo === 'LLAVE_COLOR' || tipo === 'LLAVE_DISENO'
}

export const ETIQUETA_TIPO_PRODUCTO: Record<TipoProducto, string> = {
  LLAVE_VIRGEN: 'Llave virgen',
  LLAVE_COLOR: 'Llave de color',
  LLAVE_DISENO: 'Llave de diseño',
  CHAPA: 'Chapa',
  CILINDRO: 'Cilindro',
  CANDADO: 'Candado',
  LLAVERO: 'Llavero',
  ARGOLLA: 'Argolla',
  ACCESORIO: 'Accesorio',
  REPUESTO_LICUADORA: 'Repuesto de licuadora',
}

export interface ProductoResponse {
  id: number
  codigo: string
  nombre: string
  tipoProducto: TipoProducto
  marca: string | null
  cantidadStock: number
  stockMinimo: number
  precioCompra: number
  precioVenta: number
  /** Material + corte; null si no se ofrece en "Duplicado de llave". */
  precioDuplicado: number | null
  imagenUrl: string | null
  llavePerfil: string | null
  llaveColor: string | null
  llaveDisenoTematica: string | null
  productoModelo: string | null
  productoTamano: string | null
  productoMaterial: string | null
  activo: boolean
  stockBajo: boolean
  /** Tiene su botón en "Venta rápida" (Vender). */
  ventaRapida: boolean
}

export interface ProductoCreateRequest {
  codigo: string
  nombre: string
  tipoProducto: TipoProducto
  marca?: string
  cantidadStock: number
  stockMinimo?: number
  precioCompra: number
  precioVenta: number
  imagenUrl?: string
  llavePerfil?: string
  llaveColor?: string
  llaveDisenoTematica?: string
  productoModelo?: string
  productoTamano?: string
  productoMaterial?: string
  precioDuplicado?: number | null
}

export type ProductoUpdateRequest = Omit<ProductoCreateRequest, 'codigo' | 'cantidadStock'>

export interface AjusteStockRequest {
  cantidad: number
  motivo: string
}

export interface MovimientoInventarioResponse {
  id: number
  tipoMovimiento: 'ENTRADA' | 'SALIDA' | 'AJUSTE'
  cantidad: number
  saldoAnterior: number
  saldoNuevo: number
  referenciaTipo: string | null
  referenciaId: number | null
  motivo: string | null
  usuario: string
  fechaHora: string
}

/** Refleja ImportacionResultado: la importacion es todo o nada. */
export interface ImportacionResultado {
  filasLeidas: number
  creados: number
  productos: {
    fila: number
    codigo: string
    nombre: string
    tipoProducto: TipoProducto
    cantidadStock: number
    precioVenta: number
  }[]
  errores: { fila: number; mensaje: string }[]
  columnasIgnoradas: string[]
}

export type ModoPlanilla = 'CONTEO' | 'ENTRADA'

/** Refleja inventario.dto.ResultadoPlanilla */
export interface ResultadoPlanilla {
  filasLeidas: number
  actualizados: number
  sinCambio: number
  cambios: {
    fila: number
    codigo: string
    nombre: string
    /** null si la fila no cambia el stock */
    stockAnterior: number | null
    stockNuevo: number | null
    precios: { precio: 'compra' | 'venta' | 'duplicado'; anterior: number | null; nuevo: number }[]
  }[]
  errores: { fila: number; mensaje: string }[]
}
