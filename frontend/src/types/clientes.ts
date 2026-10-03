// Refleja com.cerrajeria.sig.clientes.dto.*

export interface ClienteResponse {
  id: number
  nombre: string
  telefono: string
  direccion: string | null
  observaciones: string | null
}

export interface ClienteRequest {
  nombre: string
  telefono: string
  direccion?: string
  observaciones?: string
}
