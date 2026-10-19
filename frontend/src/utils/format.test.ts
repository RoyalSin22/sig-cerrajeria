import { describe, expect, it } from 'vitest'
import { formatearFecha, formatearSoles } from './format'

describe('formato de montos y fechas', () => {
  it('todo monto sale en soles con dos decimales', () => {
    expect(formatearSoles(12.5)).toMatch(/^S\/\s?12\.50$/)
    expect(formatearSoles(1250)).toMatch(/^S\/\s?1,250\.00$/)
  })

  it('un monto ausente se muestra como S/ 0.00, no como un número pelado ni "NaN"', () => {
    expect(formatearSoles(null)).toMatch(/^S\/\s?0\.00$/)
    expect(formatearSoles(undefined)).toMatch(/^S\/\s?0\.00$/)
  })

  it('una fecha de calendario muestra su propio día', () => {
    expect(formatearFecha('2026-10-01')).toMatch(/\b01\b/)
  })
})
