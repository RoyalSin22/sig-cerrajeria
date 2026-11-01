import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

/** El backend sirve /uploads/** fuera de /api/v1, como recurso estático. */
export const API_ORIGIN = BASE_URL.replace(/\/api\/v1\/?$/, '')

/**
 * Decision de Fase 5: el access token vive en memoria (nunca en localStorage,
 * para mitigar robo por XSS); el refresh token vive en una cookie httpOnly
 * que el navegador envia solo al backend (withCredentials: true).
 */
let accessToken: string | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export const axiosClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // envia/recibe la cookie refresh_token
})

axiosClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

// Cuando el access token expira (401), se intenta refrescar una sola vez
// antes de forzar el logout, para no interrumpir al usuario a mitad de tarea.
let refreshPromise: Promise<string | null> | null = null

async function intentarRefrescar(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = axios
      .post<{ accessToken: string }>(`${BASE_URL}/auth/refresh`, null, { withCredentials: true })
      .then((res) => {
        setAccessToken(res.data.accessToken)
        return res.data.accessToken
      })
      .catch(() => {
        setAccessToken(null)
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

axiosClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined

    if (error.response?.status === 401 && original && !original._retry && !original.url?.includes('/auth/')) {
      original._retry = true
      const nuevoToken = await intentarRefrescar()
      if (nuevoToken) {
        original.headers.Authorization = `Bearer ${nuevoToken}`
        return axiosClient(original)
      }
      // No se pudo refrescar: se limpia la sesion; el AuthContext redirige a /login.
      window.dispatchEvent(new CustomEvent('sig:sesion-expirada'))
    }

    return Promise.reject(error)
  },
)

/** Forma del error que devuelve nuestro backend (ProblemDetail, RFC 9457). */
export interface ApiProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  errores?: string
}

export function extraerMensajeError(error: unknown, mensajePorDefecto = 'Ocurrió un error inesperado'): string {
  if (axios.isAxiosError(error)) {
    const problem = error.response?.data as ApiProblemDetail | undefined
    return problem?.errores || problem?.detail || mensajePorDefecto
  }
  return mensajePorDefecto
}
