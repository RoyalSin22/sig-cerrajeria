import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Caja } from './Caja'
import { Auditoria } from './Auditoria'

/**
 * Si el cajón no cuadra hay que explicar por qué antes de cerrar, y la
 * auditoría muestra qué cambió (antes → después) y el motivo.
 */

vi.mock('../context/useCaja', () => ({
  useCaja: () => ({
    cajaAbierta: true,
    refrescarCaja: vi.fn(),
    caja: {
      id: 1, usuario: 'Johan', fechaApertura: '2026-10-21T08:00:00', fechaCierre: null, montoApertura: 50,
      montoCierre: null, montoIngresos: 50, montoEgresos: 0, montoYape: 0, montoPlin: 0, montoTarjeta: 0,
      saldoEsperado: 100, efectivoEsperado: 100, estado: 'ABIERTA', diferenciaCierre: null, observacionCierre: null,
    },
  }),
}))
vi.mock('../context/useToast', () => ({ useToast: () => ({ notificar: vi.fn() }) }))
vi.mock('../api/auditoria', () => ({
  auditoriaApi: {
    listar: vi.fn(async () => ({
      content: [{
        id: 9, usuario: 'Johan', accion: 'EDITAR', entidad: 'Producto', entidadId: 3, fechaHora: '2026-10-21T10:00:00',
        detalle: {
          descripcion: 'Precios cambiados: CASA-1 · Llave de casa',
          motivo: 'Planilla de Excel',
          cambios: { precioDuplicado: { antes: '2.00', despues: '2.50' } },
        },
      }],
      page: { number: 0, size: 20, totalElements: 1, totalPages: 1 },
    })),
  },
}))

describe('cierre de caja y auditoría', () => {
  afterEach(cleanup)

  it('si el contado no cuadra, avisa cuánto falta y pide explicación', () => {
    render(<MemoryRouter><Caja /></MemoryRouter>)
    const contado = screen.getByRole('spinbutton')

    fireEvent.change(contado, { target: { value: '100' } })
    expect(screen.getByText(/Cuadra con el efectivo esperado/)).toBeTruthy()
    expect(screen.queryByLabelText(/Qué pasó/)).toBeNull()

    fireEvent.change(contado, { target: { value: '90' } })
    expect(screen.getByText(/Faltan S\/\s?10\.00/)).toBeTruthy()
    expect((screen.getByLabelText(/Qué pasó/) as HTMLTextAreaElement).required).toBe(true)
  })

  it('la auditoría muestra la descripción, el motivo y el cambio en soles', async () => {
    render(<MemoryRouter><Auditoria /></MemoryRouter>)
    expect(await screen.findByText('Precios cambiados: CASA-1 · Llave de casa')).toBeTruthy()
    expect(screen.getByText('Planilla de Excel')).toBeTruthy()
    expect(screen.getByText(/S\/\s?2\.00 →/)).toBeTruthy()
    expect(screen.getByText(/S\/\s?2\.50/)).toBeTruthy()
  })
})
