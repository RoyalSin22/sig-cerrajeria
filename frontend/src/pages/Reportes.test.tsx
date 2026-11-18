import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { Reportes } from './Reportes'
import { analisisApi } from '../api/analisis'

/**
 * Para el dueño: el Excel de Power BI y la carga de ventas anteriores, que
 * primero se revisa y solo se guarda al confirmar. El empleado no lo ve.
 */

const usuario = vi.hoisted(() => ({ actual: { nombre: 'Juan', role: 'ROLE_ADMIN' } }))
vi.mock('../context/useAuth', () => ({ useAuth: () => ({ usuario: usuario.actual }) }))
// Los graficos no importan aca: sin datos de reportes, la pantalla igual muestra las secciones
vi.mock('../api/reportes', () => ({
  reportesApi: {
    ventas: vi.fn(async () => { throw new Error('sin datos') }),
    productosMasVendidos: vi.fn(async () => []),
    inventario: vi.fn(async () => null),
    servicios: vi.fn(async () => null),
    clientes: vi.fn(async () => null),
  },
}))
vi.mock('../api/analisis', () => ({
  analisisApi: {
    powerBi: vi.fn(async () => new Blob(['x'])),
    resumenVentasAnteriores: vi.fn(async () => ({ filas: 0, total: 0, desde: null, hasta: null })),
    cargarVentasAnteriores: vi.fn(async (_archivo: File, confirmar: boolean) => ({
      filasLeidas: 2, cargadas: confirmar ? 2 : 0, enlazadas: 1, total: 55, desde: '2025-03-15', hasta: '2025-03-16',
      muestra: [
        { fila: 2, fecha: '2025-03-15', producto: 'Llave de casa', enlazada: true, cantidad: 10, precioUnitario: 2.5, metodoPago: 'EFECTIVO' },
        { fila: 3, fecha: '2025-03-16', producto: 'Candado', enlazada: false, cantidad: 1, precioUnitario: 30, metodoPago: null },
      ],
      errores: [], columnasIgnoradas: [],
    })),
    borrarVentasAnteriores: vi.fn(),
  },
}))

describe('Reportes: Power BI y ventas anteriores', () => {
  afterEach(cleanup)

  it('revisa el archivo de ventas anteriores y solo lo guarda al confirmar', async () => {
    render(<Reportes />)
    expect(await screen.findByRole('button', { name: /Descargar Excel/ })).toBeTruthy()

    const archivo = new File(['fecha;producto'], 'ventas.csv', { type: 'text/csv' })
    fireEvent.change(screen.getByLabelText(/Elegir archivo/), { target: { files: [archivo] } })

    expect(await screen.findByText('Llave de casa')).toBeTruthy()
    expect(analisisApi.cargarVentasAnteriores).toHaveBeenLastCalledWith(archivo, false)
    fireEvent.click(screen.getByRole('button', { name: 'Cargar 2 ventas' }))

    await waitFor(() => expect(analisisApi.cargarVentasAnteriores).toHaveBeenLastCalledWith(archivo, true))
    expect(await screen.findByText(/se cargaron 2 ventas/)).toBeTruthy()
  })

  it('el empleado no ve los datos del negocio', async () => {
    usuario.actual = { nombre: 'Pedro', role: 'ROLE_EMPLOYEE' }
    render(<Reportes />)
    await screen.findByRole('heading', { name: 'Reportes' })
    expect(screen.queryByRole('button', { name: /Descargar Excel/ })).toBeNull()
    expect(screen.queryByText(/Ventas de antes del sistema/)).toBeNull()
  })
})
