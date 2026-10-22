import { useState, type ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import {
  Search, LayoutGrid, ShoppingCart, Package, Wrench, Truck,
  Users, ShoppingBag, Wallet, BarChart3, Settings, LogOut, MoreHorizontal, X,
} from 'lucide-react'
import { useAuth } from '../context/useAuth'
import { useCaja } from '../context/useCaja'
import { AsistenteChat } from '../components/AsistenteChat'

type Item = { to: string; label: string; icon: typeof LayoutGrid; end?: boolean; corto?: string }

/** Todo el menu, en el orden en que se usa en el dia. En la PC se ve siempre a la izquierda. */
const MENU: Item[] = [
  { to: '/', label: 'Inicio', icon: LayoutGrid, end: true },
  { to: '/venta', label: 'Vender', icon: ShoppingCart },
  { to: '/taller', label: 'Taller', icon: Wrench },
  { to: '/inventario', label: 'Inventario', icon: Package, corto: 'Stock' },
  { to: '/domicilio', label: 'Domicilio', icon: Truck },
  { to: '/caja', label: 'Caja', icon: Wallet },
  { to: '/clientes', label: 'Clientes', icon: Users },
  { to: '/compras', label: 'Compras', icon: ShoppingBag },
  { to: '/reportes', label: 'Reportes', icon: BarChart3 },
  { to: '/ajustes', label: 'Ajustes', icon: Settings },
]

/** En el celular, abajo y al alcance del pulgar: los 4 de todos los dias y "Más". */
const BARRA_INFERIOR = MENU.slice(0, 4)
const EN_MAS = MENU.slice(4)

export function AppLayout({ children }: { children: ReactNode }) {
  const [masAbierto, setMasAbierto] = useState(false)
  const { usuario, logout } = useAuth()
  const { cajaAbierta, cargando } = useCaja()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <div className="flex min-h-screen bg-paper">
      {/* PC: menu lateral siempre visible, sin tener que abrir nada */}
      <aside className="sticky top-0 hidden h-screen w-60 shrink-0 flex-col gap-1 overflow-y-auto border-r border-hair bg-card p-3 md:flex">
        <span className="px-3 pb-3 pt-1 font-heading text-xl font-bold text-steel-dark">Cerrajería</span>
        {MENU.map((item) => (
          <EnlaceMenu key={item.to} item={item} />
        ))}
        <button
          onClick={handleLogout}
          className="mt-auto flex min-h-12 items-center gap-3 rounded-lg px-3 text-lg font-semibold text-danger hover:bg-hair"
        >
          <LogOut size={24} aria-hidden /> Salir
        </button>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        {/* Barra superior: estado de la caja siempre a la vista (regla de negocio 1) */}
        <header className="sticky top-0 z-30 flex items-center justify-between gap-2 border-b border-hair bg-paper/95 px-4 py-2 backdrop-blur">
          <button
            type="button"
            onClick={() => navigate('/caja')}
            className={`inline-flex min-h-11 items-center gap-2 rounded-full px-4 font-semibold ${
              cargando ? 'bg-hair text-sub' : cajaAbierta ? 'bg-tint-mint text-success' : 'bg-tint-peach text-warning'
            }`}
          >
            <span
              aria-hidden
              className={`h-3 w-3 rounded-full ${cargando ? 'bg-sub' : cajaAbierta ? 'bg-success' : 'bg-warning'}`}
            />
            {cargando ? 'Verificando…' : cajaAbierta ? 'Caja abierta' : 'Caja cerrada'}
          </button>

          <div className="flex items-center gap-2">
            <span className="hidden font-semibold text-sub sm:inline">{usuario?.nombre}</span>
            <button
              type="button"
              onClick={() => navigate('/buscar')}
              className="inline-flex min-h-11 items-center gap-2 rounded-lg border-2 border-line bg-card px-3 font-semibold hover:bg-hair"
            >
              <Search size={22} aria-hidden /> <span className="hidden sm:inline">Buscar</span>
              <span className="sr-only sm:hidden">Buscar</span>
            </button>
          </div>
        </header>

        {/* pb-28 en el celular: el contenido no queda debajo de la barra inferior */}
        {import.meta.env.VITE_DEMO === '1' && (
          <p className="bg-tint-peach px-4 py-2 text-center text-sm font-semibold text-warning">
            Demostración con datos de ejemplo: se puede tocar todo, nada se guarda.
          </p>
        )}
        <main className="flex-1 px-4 pb-28 pt-4 md:px-8 md:pb-8">
          <div className="mx-auto w-full max-w-5xl">{children}</div>
        </main>
      </div>

      {/* Celular: barra inferior con iconos grandes y nombre debajo */}
      <nav
        aria-label="Menú principal"
        className="fixed inset-x-0 bottom-0 z-30 grid h-[4.75rem] grid-cols-5 border-t border-hair bg-card pb-[env(safe-area-inset-bottom)] md:hidden"
      >
        {BARRA_INFERIOR.map(({ to, label, icon: Icon, end, corto }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `flex flex-col items-center justify-center gap-0.5 text-sm font-semibold ${
                isActive ? 'bg-tint-sky text-steel-dark' : 'text-sub'
              }`
            }
          >
            <Icon size={26} aria-hidden />
            {/* "Inventario" no entra en la quinta parte de un celular: ahi dice "Stock" */}
            {corto ?? label}
          </NavLink>
        ))}
        <button
          type="button"
          onClick={() => setMasAbierto(true)}
          className="flex flex-col items-center justify-center gap-0.5 text-sm font-semibold text-sub"
        >
          <MoreHorizontal size={26} aria-hidden />
          Más
        </button>
      </nav>

      {/* "Más" en el celular: el resto del menu, en botones grandes */}
      {masAbierto && (
        <div className="fixed inset-0 z-40 flex items-end md:hidden">
          <div className="absolute inset-0 bg-ink/50" onClick={() => setMasAbierto(false)} />
          <div role="dialog" aria-modal="true" aria-label="Más opciones"
            className="panel-cepillado relative w-full rounded-t-2xl p-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
            <div className="mb-3 flex items-center justify-between">
              <span className="font-heading text-2xl font-bold">Más opciones</span>
              <button
                onClick={() => setMasAbierto(false)}
                className="inline-flex min-h-12 items-center gap-1 rounded-lg border-2 border-line px-3 font-semibold"
              >
                <X size={22} aria-hidden /> Cerrar
              </button>
            </div>
            <div className="grid grid-cols-2 gap-2">
              {EN_MAS.map(({ to, label, icon: Icon }) => (
                <NavLink
                  key={to}
                  to={to}
                  onClick={() => setMasAbierto(false)}
                  className={({ isActive }) =>
                    `flex min-h-20 flex-col items-center justify-center gap-1 rounded-xl border-2 text-lg font-semibold ${
                      isActive ? 'border-steel-dark bg-tint-sky text-steel-dark' : 'border-hair bg-card text-ink'
                    }`
                  }
                >
                  <Icon size={28} aria-hidden />
                  {label}
                </NavLink>
              ))}
              <button
                onClick={handleLogout}
                className="flex min-h-20 flex-col items-center justify-center gap-1 rounded-xl border-2 border-hair text-lg font-semibold text-danger"
              >
                <LogOut size={28} aria-hidden />
                Salir
              </button>
            </div>
            <p className="mt-3 text-center text-sm text-sub">{usuario?.nombre}</p>
          </div>
        </div>
      )}

      <AsistenteChat />
    </div>
  )
}

function EnlaceMenu({ item: { to, label, icon: Icon, end } }: { item: Item }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        `flex min-h-12 items-center gap-3 rounded-lg px-3 text-lg font-semibold ${
          isActive ? 'bg-tint-sky text-steel-dark' : 'text-ink hover:bg-hair'
        }`
      }
    >
      <Icon size={24} aria-hidden />
      {label}
    </NavLink>
  )
}
