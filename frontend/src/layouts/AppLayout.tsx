import { useState, type ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import {
  Menu, X, Search, LayoutGrid, ShoppingCart, Package, Wrench, Truck,
  Users, ShoppingBag, Wallet, BarChart3, Settings, LogOut,
} from 'lucide-react'
import { useAuth } from '../context/useAuth'
import { useCaja } from '../context/useCaja'
import { AsistenteChat } from '../components/AsistenteChat'

const PESTANAS = [
  { to: '/', label: 'Inicio', icon: LayoutGrid, end: true },
  { to: '/venta', label: 'Venta', icon: ShoppingCart },
  { to: '/inventario', label: 'Inventario', icon: Package },
  { to: '/taller', label: 'Taller', icon: Wrench },
  { to: '/domicilio', label: 'Domicilio', icon: Truck },
]

const DRAWER = [
  { to: '/clientes', label: 'Clientes', icon: Users },
  { to: '/compras', label: 'Compras', icon: ShoppingBag },
  { to: '/caja', label: 'Caja', icon: Wallet },
  { to: '/reportes', label: 'Reportes', icon: BarChart3 },
  { to: '/ajustes', label: 'Ajustes', icon: Settings },
]

export function AppLayout({ children }: { children: ReactNode }) {
  const [drawerAbierto, setDrawerAbierto] = useState(false)
  const { usuario, logout } = useAuth()
  const { cajaAbierta, cargando } = useCaja()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <div className="flex min-h-screen flex-col bg-paper">
      {/* Barra superior persistente */}
      <header className="sticky top-0 z-30 flex items-center justify-between border-b border-hair bg-paper/95 px-4 py-3 backdrop-blur">
        <div className="flex items-center gap-3">
          <button
            type="button"
            aria-label="Abrir menú"
            onClick={() => setDrawerAbierto(true)}
            className="rounded-md p-1.5 hover:bg-hair"
          >
            <Menu size={22} />
          </button>
          <span className="font-heading text-lg font-bold tracking-wide text-steel-dark">
            SIG-CERRAJERÍA
          </span>
        </div>

        <div className="flex items-center gap-4">
          <button
            type="button"
            aria-label="Buscar"
            onClick={() => navigate('/buscar')}
            className="rounded-md p-1.5 hover:bg-hair"
          >
            <Search size={20} />
          </button>

          {/* Estado de caja: siempre visible, refuerza la regla de negocio 1 */}
          <span className="hidden items-center gap-1.5 text-sm sm:flex">
            <span
              className={`h-2 w-2 rounded-full ${
                cargando ? 'bg-sub' : cajaAbierta ? 'bg-success' : 'bg-danger'
              }`}
            />
            {cargando ? 'Verificando…' : cajaAbierta ? 'Caja abierta' : 'Caja cerrada'}
          </span>

          <span className="hidden text-sm text-sub md:inline">{usuario?.nombre}</span>
        </div>
      </header>

      {/* Pestañas principales */}
      <nav className="sticky top-[57px] z-20 flex gap-1 overflow-x-auto border-b border-hair bg-paper px-2">
        {PESTANAS.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `flex shrink-0 items-center gap-1.5 border-b-2 px-3.5 py-2.5 text-sm font-medium transition-colors ${
                isActive
                  ? 'border-steel-dark text-steel-dark'
                  : 'border-transparent text-sub hover:text-ink'
              }`
            }
          >
            <Icon size={16} />
            {label}
          </NavLink>
        ))}
      </nav>

      <main className="flex-1 px-4 py-4 md:px-6">{children}</main>

      {/* Menú lateral (drawer) */}
      {drawerAbierto && (
        <div className="fixed inset-0 z-40 flex">
          <div className="absolute inset-0 bg-ink/40" onClick={() => setDrawerAbierto(false)} />
          <aside className="relative flex h-full w-72 flex-col gap-1 bg-paper p-4 shadow-xl">
            <div className="mb-4 flex items-center justify-between">
              <span className="font-heading text-lg font-bold">Menú</span>
              <button onClick={() => setDrawerAbierto(false)} aria-label="Cerrar menú">
                <X size={20} />
              </button>
            </div>
            {DRAWER.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                onClick={() => setDrawerAbierto(false)}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium ${
                    isActive ? 'bg-tint-sky text-steel-dark' : 'text-ink hover:bg-hair'
                  }`
                }
              >
                <Icon size={18} />
                {label}
              </NavLink>
            ))}

            <button
              onClick={handleLogout}
              className="mt-auto flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium text-danger hover:bg-hair"
            >
              <LogOut size={18} />
              Cerrar sesión
            </button>
          </aside>
        </div>
      )}

      <AsistenteChat />
    </div>
  )
}
