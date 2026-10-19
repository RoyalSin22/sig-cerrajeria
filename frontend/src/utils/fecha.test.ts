import { describe, expect, it } from 'vitest'
import { aFechaISO, finMesISO, inicioMesISO, parsearFechaISO, sumarDiasISO, sumarMesesISO } from './fecha'

// vitest.config.ts fija TZ=America/Lima (UTC-5), como la PC del local.
describe('fechas de calendario en hora de Perú', () => {
  it('a las 20:30 del 3 de octubre sigue siendo 3 de octubre (toISOString ya diría 4)', () => {
    const nocheEnLima = new Date(2026, 9, 3, 20, 30)
    expect(aFechaISO(nocheEnLima)).toBe('2026-10-03')
    expect(nocheEnLima.toISOString().slice(0, 10)).toBe('2026-10-04') // el error que evita fecha.ts
  })

  it('"2026-10-01" se lee como 1 de octubre, no como 30 de septiembre', () => {
    const d = parsearFechaISO('2026-10-01')
    expect(d.getDate()).toBe(1)
    expect(d.getMonth()).toBe(9)
    expect(new Date('2026-10-01').getDate()).toBe(30) // lo que haría new Date(texto)
  })

  it('suma días cruzando meses y años', () => {
    expect(sumarDiasISO('2026-10-31', 1)).toBe('2026-11-01')
    expect(sumarDiasISO('2026-12-31', 1)).toBe('2027-01-01')
    expect(sumarDiasISO('2026-03-01', -1)).toBe('2026-02-28')
  })

  it('sumar un mes desde el 31 no salta febrero', () => {
    expect(sumarMesesISO('2026-01-31', 1)).toBe('2026-02-01')
    expect(sumarMesesISO('2026-01-15', -1)).toBe('2025-12-01')
  })

  it('inicio y fin de mes, con febrero bisiesto', () => {
    expect(inicioMesISO('2026-10-17')).toBe('2026-10-01')
    expect(finMesISO('2026-02-10')).toBe('2026-02-28')
    expect(finMesISO('2028-02-10')).toBe('2028-02-29')
  })
})
