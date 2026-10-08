// Refleja com.cerrajeria.sig.inventario.dto.* y TipoProducto

export type TipoProducto =
  | 'LLAVE_VIRGEN' | 'LLAVE_COLOR' | 'LLAVE_DISENO' | 'CHAPA' | 'CILINDRO'
  | 'CANDADO' | 'LLAVERO' | 'ARGOLLA' | 'ACCESORIO'

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
  imagenUrl: string | null
  llavePerfil: string | null
  llaveColor: string | null
  llaveDisenoTematica: string | null
  productoModelo: string | null
  productoTamano: string | null
  productoMaterial: string | null
  activo: boolean
  stockBajo: boolean
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
