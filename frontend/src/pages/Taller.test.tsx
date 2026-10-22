import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Taller } from './Taller'

/** Un trabajo listo se avisa al cliente por WhatsApp con el mensaje ya escrito. */

const orden = {
  id: 3, numeroOrden: 'OT-000003', cliente: 'Rosa Quispe', productoRecibido: 'Licuadora Oster', problema: 'No corta',
  diagnostico: null, precioTotal: 35, montoPagado: 15, saldoPendiente: 20, estado: 'LISTO', fechaIngreso: '2026-10-22T10:00:00',
  fechaPrometida: null, fechaEntrega: null, observaciones: null, motivoCancelacion: null, garantia: false,
  numeroOrdenOrigen: null, telefonoCliente: '987654321',
}

vi.mock('../context/useToast', () => ({ useToast: () => ({ notificar: vi.fn() }) }))
vi.mock('../api/ordenes', () => ({
  ordenesApi: { listar: vi.fn(async () => [orden]), materiales: vi.fn(async () => []) },
}))
vi.mock('../api/negocio', () => ({
  negocioApi: { obtener: vi.fn(async () => ({ nombre: 'Cerrajería Barrera', telefono: '982616709', direccion: 'Av. Alfonso Ugarte 115' })) },
}))

describe('Taller: avisos por WhatsApp', () => {
  afterEach(cleanup)

  it('un trabajo listo tiene el botón para avisar, con el saldo y los datos del negocio', async () => {
    render(<MemoryRouter><Taller /></MemoryRouter>)
    fireEvent.click(await screen.findByText('Rosa Quispe'))

    const aviso = screen.getByRole('link', { name: /Avisar que ya está listo/ }) as HTMLAnchorElement
    const texto = decodeURIComponent(aviso.href.split('?text=')[1])
    expect(aviso.href).toMatch(/^https:\/\/wa\.me\/51987654321\?text=/)
    expect(texto).toContain('ya está listo su trabajo: Licuadora Oster (orden OT-000003)')
    expect(texto).toMatch(/Saldo: S\/\s?20\.00/)
    expect(texto).toContain('Cerrajería Barrera')
    expect(screen.getByRole('link', { name: /Enviar comprobante/ })).toBeTruthy()
  })
})
