import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AppLayout } from './layouts/AppLayout'
import { Login } from './pages/Login'

// Cada pantalla se descarga al visitarla (code splitting): el bundle inicial ya no
// arrastra Recharts ni pantallas que el empleado quiza no abra en el dia.
const Dashboard = lazy(() => import('./pages/Dashboard').then((m) => ({ default: m.Dashboard })))
const Caja = lazy(() => import('./pages/Caja').then((m) => ({ default: m.Caja })))
const HistorialCaja = lazy(() => import('./pages/HistorialCaja').then((m) => ({ default: m.HistorialCaja })))
const Venta = lazy(() => import('./pages/Venta').then((m) => ({ default: m.Venta })))
const HistorialVentas = lazy(() => import('./pages/HistorialVentas').then((m) => ({ default: m.HistorialVentas })))
const Inventario = lazy(() => import('./pages/Inventario').then((m) => ({ default: m.Inventario })))
const Clientes = lazy(() => import('./pages/Clientes').then((m) => ({ default: m.Clientes })))
const Taller = lazy(() => import('./pages/Taller').then((m) => ({ default: m.Taller })))
const Domicilio = lazy(() => import('./pages/Domicilio').then((m) => ({ default: m.Domicilio })))
const Compras = lazy(() => import('./pages/Compras').then((m) => ({ default: m.Compras })))
const ListaCompras = lazy(() => import('./pages/ListaCompras').then((m) => ({ default: m.ListaCompras })))
const HistorialCompras = lazy(() => import('./pages/HistorialCompras').then((m) => ({ default: m.HistorialCompras })))
const Proveedores = lazy(() => import('./pages/Proveedores').then((m) => ({ default: m.Proveedores })))
const Reportes = lazy(() => import('./pages/Reportes').then((m) => ({ default: m.Reportes })))
const Buscar = lazy(() => import('./pages/Buscar').then((m) => ({ default: m.Buscar })))
const Ajustes = lazy(() => import('./pages/Ajustes').then((m) => ({ default: m.Ajustes })))
const Auditoria = lazy(() => import('./pages/Auditoria').then((m) => ({ default: m.Auditoria })))

function ConLayout({ children, soloRol }: { children: React.ReactNode; soloRol?: 'ROLE_ADMIN' }) {
  return (
    <ProtectedRoute soloRol={soloRol}>
      <AppLayout>
        <Suspense fallback={<p className="py-10 text-center text-sm text-sub">Cargando…</p>}>{children}</Suspense>
      </AppLayout>
    </ProtectedRoute>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route path="/" element={<ConLayout><Dashboard /></ConLayout>} />
      <Route path="/venta" element={<ConLayout><Venta /></ConLayout>} />
      <Route path="/venta/historial" element={<ConLayout><HistorialVentas /></ConLayout>} />
      <Route path="/inventario" element={<ConLayout><Inventario /></ConLayout>} />
      <Route path="/taller" element={<ConLayout><Taller /></ConLayout>} />
      <Route path="/domicilio" element={<ConLayout><Domicilio /></ConLayout>} />
      <Route path="/clientes" element={<ConLayout><Clientes /></ConLayout>} />
      <Route path="/compras" element={<ConLayout><Compras /></ConLayout>} />
      <Route path="/compras/historial" element={<ConLayout><HistorialCompras /></ConLayout>} />
      <Route path="/compras/lista" element={<ConLayout><ListaCompras /></ConLayout>} />
      <Route path="/proveedores" element={<ConLayout><Proveedores /></ConLayout>} />
      <Route path="/caja" element={<ConLayout><Caja /></ConLayout>} />
      <Route path="/caja/historial" element={<ConLayout><HistorialCaja /></ConLayout>} />
      <Route path="/reportes" element={<ConLayout><Reportes /></ConLayout>} />
      <Route path="/buscar" element={<ConLayout><Buscar /></ConLayout>} />
      <Route path="/ajustes" element={<ConLayout><Ajustes /></ConLayout>} />
      <Route path="/auditoria" element={<ConLayout soloRol="ROLE_ADMIN"><Auditoria /></ConLayout>} />
    </Routes>
  )
}
