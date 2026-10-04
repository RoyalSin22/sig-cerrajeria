import { API_ORIGIN } from '../api/axiosClient'
import { parsearFechaISO } from './fecha'

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

const formateadorDia = new Intl.DateTimeFormat('es-PE', {
  weekday: 'short',
  day: '2-digit',
  month: 'short',
})

/** Fecha sin hora ("2026-10-04"), parseada como fecha local (ver utils/fecha.ts). */
export function formatearFecha(fechaISO: string): string {
  return formateadorDia.format(parsearFechaISO(fechaISO))
}

const formateadorHora = new Intl.DateTimeFormat('es-PE', { hour: '2-digit', minute: '2-digit' })

/** Hora sin fecha ("10:30:00"). */
export function formatearHora(hora: string): string {
  const [h, m] = hora.split(':').map(Number)
  return formateadorHora.format(new Date(2000, 0, 1, h, m))
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
