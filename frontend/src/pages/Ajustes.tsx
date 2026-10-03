import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Moon, Sun } from 'lucide-react'
import { usuariosApi } from '../api/usuarios'
import { extraerMensajeError } from '../api/axiosClient'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { useToast } from '../context/ToastContext'
import type { Rol, UsuarioResponse } from '../types/usuarios'

export function Ajustes() {
  const { usuario } = useAuth()
  const { tema, alternarTema } = useTheme()
  const navigate = useNavigate()

  return (
    <div className="mx-auto flex max-w-lg flex-col gap-5">
      <h1 className="font-heading text-2xl font-semibold">Ajustes</h1>

      <Card className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium">Sesión</p>
          <p className="text-xs text-sub">{usuario?.nombre} · {usuario?.role === 'ROLE_ADMIN' ? 'Administrador' : 'Empleado'}</p>
        </div>
      </Card>

      <Card className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium">Apariencia</p>
          <p className="text-xs text-sub">Modo {tema}</p>
        </div>
        <Button variant="secondary" onClick={alternarTema} className="flex items-center gap-2">
          {tema === 'claro' ? <Moon size={16} /> : <Sun size={16} />}
          Cambiar
        </Button>
      </Card>

      {usuario?.role === 'ROLE_ADMIN' && (
        <Card className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium">Auditoría</p>
            <p className="text-xs text-sub">Registro de acciones sensibles del sistema</p>
          </div>
          <Button variant="secondary" onClick={() => navigate('/auditoria')}>Ver</Button>
        </Card>
      )}

      {usuario?.role === 'ROLE_ADMIN' && <GestionUsuarios />}
    </div>
  )
}

function GestionUsuarios() {
  const { notificar } = useToast()
  const [usuarios, setUsuarios] = useState<UsuarioResponse[]>([])
  const [mostrarAlta, setMostrarAlta] = useState(false)

  async function cargar() {
    setUsuarios(await usuariosApi.listar())
  }

  useEffect(() => {
    cargar()
  }, [])

  async function cambiarEstado(id: number, activo: boolean) {
    const actualizado = await usuariosApi.cambiarEstado(id, activo)
    setUsuarios((prev) => prev.map((u) => (u.id === id ? actualizado : u)))
  }

  return (
    <Card className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <p className="font-heading text-lg font-semibold">Usuarios</p>
        <Button onClick={() => setMostrarAlta((v) => !v)}>{mostrarAlta ? 'Cancelar' : 'Nuevo usuario'}</Button>
      </div>

      {mostrarAlta && (
        <FormularioUsuario
          onCreado={(u) => {
            notificar('Usuario creado')
            setUsuarios((prev) => [...prev, u])
            setMostrarAlta(false)
          }}
        />
      )}

      <div className="flex flex-col divide-y divide-hair">
        {usuarios.map((u) => (
          <div key={u.id} className="flex items-center justify-between py-2.5 text-sm">
            <div>
              <p className="font-medium">{u.nombre}</p>
              <p className="text-xs text-sub">@{u.username} · {u.role === 'ROLE_ADMIN' ? 'Administrador' : 'Empleado'}</p>
            </div>
            <div className="flex items-center gap-2">
              <Badge tono={u.activo ? 'success' : 'neutral'}>{u.activo ? 'Activo' : 'Inactivo'}</Badge>
              <button
                onClick={() => cambiarEstado(u.id, !u.activo)}
                className="text-xs text-steel underline"
              >
                {u.activo ? 'Desactivar' : 'Activar'}
              </button>
            </div>
          </div>
        ))}
      </div>
    </Card>
  )
}

function FormularioUsuario({ onCreado }: { onCreado: (u: UsuarioResponse) => void }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [nombre, setNombre] = useState('')
  const [role, setRole] = useState<Rol>('ROLE_EMPLOYEE')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setGuardando(true)
    try {
      const nuevo = await usuariosApi.crear({ username, password, nombre, role })
      onCreado(nuevo)
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-2 rounded-md border border-line p-3">
      <input required value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Nombre completo"
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" />
      <input required value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Usuario"
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" />
      <input required type="password" minLength={6} value={password} onChange={(e) => setPassword(e.target.value)}
        placeholder="Contraseña (mínimo 6 caracteres)"
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" />
      <select value={role} onChange={(e) => setRole(e.target.value as Rol)}
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel">
        <option value="ROLE_EMPLOYEE">Empleado</option>
        <option value="ROLE_ADMIN">Administrador</option>
      </select>
      {error && <p className="text-sm text-danger">{error}</p>}
      <Button type="submit" disabled={guardando}>{guardando ? 'Creando…' : 'Crear usuario'}</Button>
    </form>
  )
}
