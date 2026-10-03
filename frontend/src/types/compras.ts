// Refleja com.cerrajeria.sig.compras.dto.*

export interface ProveedorRequest {
  nombre: string
  telefono?: string
  direccion?: string
}

export interface ProveedorResponse {
  id: number
  nombre: string
  telefono: string | null
  direccion: string | null
}

export interface CompraCreateRequest {
  proveedorId: number
  productoId: number
  cantidad: number
  precioUnitario: number
  pagarDeCaja: boolean
}

export interface CompraResponse {
  id: number
  proveedor: string
  producto: string
  cantidad: number
  precioUnitario: number
  precioTotal: number
  fecha: string
}
