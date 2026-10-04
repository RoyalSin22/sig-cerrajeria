/**
 * Fechas sin hora ("2026-10-04"), siempre en hora local.
 *
 * No usar new Date('2026-10-04') ni toISOString() para fechas de calendario:
 * la primera interpreta el texto como medianoche UTC y la segunda devuelve la
 * fecha UTC. En Peru (UTC-5) eso corre el dia: a las 20:30 del 3 de octubre
 * toISOString() ya dice 4 de octubre, y "2026-10-01" cae el 30 de septiembre.
 */

/** Convierte "AAAA-MM-DD" en un Date a medianoche local. */
export function parsearFechaISO(fechaISO: string): Date {
  const [anio, mes, dia] = fechaISO.split('-').map(Number)
  return new Date(anio, mes - 1, dia)
}

/** Fecha local de un Date como "AAAA-MM-DD". */
export function aFechaISO(fecha: Date): string {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const dia = String(fecha.getDate()).padStart(2, '0')
  return `${fecha.getFullYear()}-${mes}-${dia}`
}

export function hoyISO(): string {
  return aFechaISO(new Date())
}

export function sumarDiasISO(fechaISO: string, dias: number): string {
  const d = parsearFechaISO(fechaISO)
  d.setDate(d.getDate() + dias)
  return aFechaISO(d)
}

/** Suma meses quedandose en el dia 1, para no saltar meses cortos (31-ene + 1 mes). */
export function sumarMesesISO(fechaISO: string, meses: number): string {
  const d = parsearFechaISO(fechaISO)
  return aFechaISO(new Date(d.getFullYear(), d.getMonth() + meses, 1))
}

export function inicioMesISO(fechaISO: string): string {
  const d = parsearFechaISO(fechaISO)
  return aFechaISO(new Date(d.getFullYear(), d.getMonth(), 1))
}

export function finMesISO(fechaISO: string): string {
  const d = parsearFechaISO(fechaISO)
  return aFechaISO(new Date(d.getFullYear(), d.getMonth() + 1, 0))
}
