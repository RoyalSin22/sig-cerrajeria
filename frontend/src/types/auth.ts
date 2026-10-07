// Refleja com.cerrajeria.sig.auth.dto.* y com.cerrajeria.sig.usuarios.Role

export type Rol = 'ROLE_ADMIN' | 'ROLE_EMPLOYEE'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  username: string
  nombre: string
  role: Rol
}

export interface UsuarioAutenticado {
  username: string
  nombre: string
  role: Rol
}
