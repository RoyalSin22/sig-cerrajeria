import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Venta } from './Venta'
import { ventasApi } from '../api/ventas'
import { productosApi } from '../api/productos'

/**
 * Una llave se cobra casi siempre como duplicado (material + corte) y muy
 * rara vez sola: son dos precios y la venta le dice al backend cuál es cuál.
 */

const llaveCasa = {
  id: 7, codigo: 'CASA-1', nombre: 'Llave de casa', tipoProducto: 'LLAVE_VIRGEN', marca: null,
  cantidadStock: 500, stockMinimo: 100, precioCompra: 0.5, precioVenta: 0.9, precioDuplicado: 2.5,
}

const llavero = {
  id: 9, codigo: 'LLAVERO', nombre: 'Llavero metálico', tipoProducto: 'LLAVERO', marca: null,
  cantidadStock: 40, stockMinimo: 10, precioCompra: 1.5, precioVenta: 4, precioDuplicado: null, ventaRapida: true,
}

vi.mock('../context/useCaja', () => ({ useCaja: () => ({ cajaAbierta: { id: 1 }, refrescarCaja: vi.fn() }) }))
vi.mock('../context/useToast', () => ({ useToast: () => ({ notificar: vi.fn() }) }))
vi.mock('../api/productos', () => ({
  productosApi: {
    buscar: vi.fn(async () => [llaveCasa]),
    duplicables: vi.fn(async () => [llaveCasa]),
    ventaRapida: vi.fn(async () => [llaveCasa, llavero]),
  },
}))
vi.mock('../api/ventas', () => ({ ventasApi: { crear: vi.fn(async () => ({})) } }))

describe('Venta: duplicado y llave sola', () => {
  afterEach(cleanup)

  it('al buscar una llave ofrece el duplicado y la llave sola, cada uno con su precio', async () => {
    vi.mocked(productosApi.ventaRapida).mockResolvedValueOnce([]) // sin botones: que no se confundan con el buscador
    render(<MemoryRouter><Venta /></MemoryRouter>)
    fireEvent.change(screen.getByPlaceholderText(/Buscar otro producto/), { target: { value: 'casa' } })

    fireEvent.click(await screen.findByRole('button', { name: /Duplicado S\/\s?2\.50/ }))
    fireEvent.click(screen.getByRole('button', { name: /Duplicado S\/\s?2\.50/ }))
    fireEvent.click(screen.getByRole('button', { name: /Sola S\/\s?0\.90/ }))

    expect(screen.getByText('Duplicado ·')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /^Cobrar/ }))

    expect(ventasApi.crear).toHaveBeenCalledWith(expect.objectContaining({
      items: [
        { productoId: 7, cantidad: 2, duplicado: true },
        { productoId: 7, cantidad: 1, duplicado: false },
      ],
    }))
  })

  it('duplicado en tres toques: botón, llave y cantidad; el total aparece en el botón Cobrar', async () => {
    vi.mocked(productosApi.ventaRapida).mockResolvedValueOnce([])
    render(<MemoryRouter><Venta /></MemoryRouter>)
    fireEvent.click(screen.getByRole('button', { name: /Duplicado de llave/ }))
    fireEvent.click(await screen.findByRole('button', { name: /Llave de casa/ }))
    fireEvent.click(screen.getByRole('button', { name: /^3\s*S\/\s?7\.50/ }))

    expect(screen.getByRole('button', { name: /Cobrar S\/\s?7\.50/ })).toBeTruthy()
  })

  it('venta rápida: un toque por botón y Cobrar; la llave va como duplicado', async () => {
    vi.mocked(ventasApi.crear).mockClear()
    render(<MemoryRouter><Venta /></MemoryRouter>)
    fireEvent.click(await screen.findByRole('button', { name: /Llavero metálico/ }))
    fireEvent.click(screen.getByRole('button', { name: /Llavero metálico/ }))
    fireEvent.click(screen.getByRole('button', { name: /Duplicado ·\s?Llave de casa/ }))
    fireEvent.click(screen.getByRole('button', { name: /Cobrar S\/\s?10\.50/ }))

    expect(ventasApi.crear).toHaveBeenCalledWith(expect.objectContaining({
      items: [
        { productoId: 9, cantidad: 2, duplicado: false },
        { productoId: 7, cantidad: 1, duplicado: true },
      ],
    }))
  })

  it('servicio rápido sin producto: se elige, se pone el precio y se cobra', async () => {
    vi.mocked(ventasApi.crear).mockClear()
    render(<MemoryRouter><Venta /></MemoryRouter>)
    fireEvent.click(screen.getByRole('button', { name: /^Servicio$/ }))
    fireEvent.click(screen.getByRole('button', { name: 'Abrir candado' }))
    fireEvent.change(screen.getByLabelText(/Precio/), { target: { value: '10' } })
    fireEvent.click(screen.getByRole('button', { name: /Agregar S\/\s?10\.00/ }))
    fireEvent.click(screen.getByRole('button', { name: /Cobrar S\/\s?10\.00/ }))

    expect(ventasApi.crear).toHaveBeenCalledWith(expect.objectContaining({
      items: [{ descripcion: 'Abrir candado', precio: 10, cantidad: 1 }],
    }))
  })
})
