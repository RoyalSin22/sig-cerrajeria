import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AppLayout } from './layouts/AppLayout'
import { Login } from './pages/Login'
import { Dashboard } from './pages/Dashboard'
import { Caja } from './pages/Caja'
import { HistorialCaja } from './pages/HistorialCaja'
import { Venta } from './pages/Venta'
import { HistorialVentas } from './pages/HistorialVentas'
import { Inventario } from './pages/Inventario'
import { Clientes } from './pages/Clientes'
import { Taller } from './pages/Taller'
import { Domicilio } from './pages/Domicilio'
import { Compras } from './pages/Compras'
import { HistorialCompras } from './pages/HistorialCompras'
import { Proveedores } from './pages/Proveedores'
import { Reportes } from './pages/Reportes'
import { Buscar } from './pages/Buscar'
import { Ajustes } from './pages/Ajustes'
import { Auditoria } from './pages/Auditoria'

function ConLayout({ children, soloRol }: { children: React.ReactNode; soloRol?: 'ROLE_ADMIN' }) {
  return (
    <ProtectedRoute soloRol={soloRol}>
      <AppLayout>{children}</AppLayout>
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
