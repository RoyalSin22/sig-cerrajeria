import { API_ORIGIN } from '../api/axiosClient'

const formateadorSoles = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
})

/** Fase 1 §2 / Fase 7: todo monto del sistema se muestra en soles, nunca en otra moneda. */
export function formatearSoles(monto: number | null | undefined): string {
  return formateadorSoles.format(monto ?? 0)
}

const formateadorFecha = new Intl.DateTimeFormat('es-PE', {
  day: '2-digit',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
})

export function formatearFechaHora(iso: string): string {
  return formateadorFecha.format(new Date(iso))
}

/**
 * El backend devuelve rutas relativas para las imagenes de productos
 * (ej. "/uploads/productos/12.jpg?v=..."). Se le antepone el origen del
 * backend (sin /api/v1) para armar una URL cargable en un <img>.
 */
export function urlImagen(path: string | null | undefined): string | null {
  if (!path) return null
  if (path.startsWith('http')) return path
  return `${API_ORIGIN}${path}`
}
