// Refleja com.cerrajeria.sig.usuarios.dto.*

export type Rol = 'ROLE_ADMIN' | 'ROLE_EMPLOYEE'

export interface UsuarioResponse {
  id: number
  username: string
  nombre: string
  role: Rol
  activo: boolean
}

export interface UsuarioCreateRequest {
  username: string
  password: string
  nombre: string
  role: Rol
}
