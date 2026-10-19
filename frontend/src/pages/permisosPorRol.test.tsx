import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Inventario } from './Inventario'
import { Compras } from './Compras'

/**
 * El backend rechaza con 403 lo que es solo del administrador; el frontend no
 * debe mostrarle esos botones al empleado (antes los veía y le daban error).
 */

let rol: 'ROLE_ADMIN' | 'ROLE_EMPLOYEE' = 'ROLE_ADMIN'

vi.mock('../context/useAuth', () => ({
  useAuth: () => ({ usuario: { id: 1, username: 'u', nombre: 'Usuario', role: rol } }),
}))
vi.mock('../context/useToast', () => ({ useToast: () => ({ notificar: vi.fn() }) }))
vi.mock('../context/useCaja', () => ({ useCaja: () => ({ cajaAbierta: null, refrescarCaja: vi.fn() }) }))
vi.mock('../api/productos', () => ({
  productosApi: {
    listar: vi.fn().mockResolvedValue({ content: [], page: { number: 0, size: 20, totalElements: 0, totalPages: 0 } }),
    buscar: vi.fn().mockResolvedValue([]),
    stockBajo: vi.fn().mockResolvedValue([]),
  },
}))
vi.mock('../api/compras', () => ({
  comprasApi: { listarProveedores: vi.fn().mockResolvedValue([]) },
}))

function renderizar(pantalla: React.ReactNode) {
  return render(<MemoryRouter>{pantalla}</MemoryRouter>)
}

describe('permisos por rol', () => {
  beforeEach(() => {
    rol = 'ROLE_ADMIN'
  })
  afterEach(cleanup)

  it('Inventario: el administrador ve "Nuevo producto" e "Importar"', async () => {
    renderizar(<Inventario />)
    expect(await screen.findByRole('button', { name: 'Nuevo producto' })).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Importar' })).toBeTruthy()
  })

  it('Inventario: el empleado no los ve', async () => {
    rol = 'ROLE_EMPLOYEE'
    renderizar(<Inventario />)
    expect(await screen.findByRole('heading', { name: 'Inventario' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Nuevo producto' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Importar' })).toBeNull()
  })

  it('Compras: el administrador ve el formulario de registro', async () => {
    renderizar(<Compras />)
    expect(await screen.findByRole('button', { name: 'Registrar compra' })).toBeTruthy()
  })

  it('Compras: el empleado ve un aviso en vez del formulario', async () => {
    rol = 'ROLE_EMPLOYEE'
    renderizar(<Compras />)
    expect(await screen.findByText(/Solo el administrador registra compras/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Registrar compra' })).toBeNull()
  })
})
