import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Moon, Sun } from 'lucide-react'
import { usuariosApi } from '../api/usuarios'
import { extraerMensajeError } from '../api/axiosClient'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { useAuth } from '../context/useAuth'
import { useTheme } from '../context/useTheme'
import { useToast } from '../context/useToast'
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

      <CambiarMiPassword />

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

const CLASE_INPUT = 'rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel'

function CambiarMiPassword() {
  const { logout } = useAuth()
  const { notificar } = useToast()
  const navigate = useNavigate()
  const [abierto, setAbierto] = useState(false)
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [confirmacion, setConfirmacion] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (nueva !== confirmacion) {
      setError('La nueva contraseña y su confirmación no coinciden')
      return
    }
    setError(null)
    setGuardando(true)
    try {
      await usuariosApi.cambiarMiPassword(actual, nueva)
      // El cambio cierra todas las sesiones (incluida esta): se vuelve a entrar
      // con la nueva, lo que ademas confirma que quedo bien guardada.
      notificar('Contraseña cambiada. Inicia sesión con la nueva.')
      await logout()
      navigate('/login')
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <Card className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium">Contraseña</p>
          <p className="text-xs text-sub">Al cambiarla se cierran las sesiones abiertas</p>
        </div>
        <Button variant="secondary" onClick={() => setAbierto((v) => !v)}>{abierto ? 'Cancelar' : 'Cambiar'}</Button>
      </div>
      {abierto && (
        <form onSubmit={handleSubmit} className="flex flex-col gap-2">
          <input required type="password" autoComplete="current-password" value={actual}
            onChange={(e) => setActual(e.target.value)} placeholder="Contraseña actual" className={CLASE_INPUT} />
          <input required type="password" autoComplete="new-password" minLength={6} value={nueva}
            onChange={(e) => setNueva(e.target.value)} placeholder="Nueva contraseña (mínimo 6 caracteres)" className={CLASE_INPUT} />
          <input required type="password" autoComplete="new-password" minLength={6} value={confirmacion}
            onChange={(e) => setConfirmacion(e.target.value)} placeholder="Repite la nueva contraseña" className={CLASE_INPUT} />
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar nueva contraseña'}</Button>
        </form>
      )}
    </Card>
  )
}

function GestionUsuarios() {
  const { usuario: actual } = useAuth()
  const { notificar } = useToast()
  const [usuarios, setUsuarios] = useState<UsuarioResponse[]>([])
  const [mostrarAlta, setMostrarAlta] = useState(false)
  const [restableciendo, setRestableciendo] = useState<number | null>(null)
  const [passwordTemporal, setPasswordTemporal] = useState('')
  const [errorRestablecer, setErrorRestablecer] = useState<string | null>(null)

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

  async function restablecer(e: FormEvent, u: UsuarioResponse) {
    e.preventDefault()
    setErrorRestablecer(null)
    try {
      await usuariosApi.restablecerPassword(u.id, passwordTemporal)
      notificar(`Contraseña de ${u.nombre} restablecida. Entrégasela para que la cambie.`)
      setRestableciendo(null)
      setPasswordTemporal('')
    } catch (err) {
      setErrorRestablecer(extraerMensajeError(err))
    }
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
          <div key={u.id} className="flex flex-col gap-2 py-2.5 text-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium">{u.nombre}</p>
              <p className="text-xs text-sub">@{u.username} · {u.role === 'ROLE_ADMIN' ? 'Administrador' : 'Empleado'}</p>
            </div>
            <div className="flex items-center gap-2">
              <Badge tono={u.activo ? 'success' : 'neutral'}>{u.activo ? 'Activo' : 'Inactivo'}</Badge>
              {/* El backend tambien lo impide: sin admin activo nadie gestiona usuarios */}
              {u.username !== actual?.username && (
                <button
                  onClick={() => cambiarEstado(u.id, !u.activo)}
                  className="text-xs text-steel underline"
                >
                  {u.activo ? 'Desactivar' : 'Activar'}
                </button>
              )}
              {/* La propia se cambia arriba (exige la actual); aqui solo la de otros */}
              {u.username !== actual?.username && (
                <button
                  onClick={() => { setRestableciendo(restableciendo === u.id ? null : u.id); setPasswordTemporal(''); setErrorRestablecer(null) }}
                  className="text-xs text-steel underline"
                >
                  Restablecer contraseña
                </button>
              )}
            </div>
          </div>
          {restableciendo === u.id && (
            <form onSubmit={(e) => restablecer(e, u)} className="flex flex-col gap-2 rounded-md border border-line p-3">
              <input required type="password" autoComplete="new-password" minLength={6} value={passwordTemporal}
                onChange={(e) => setPasswordTemporal(e.target.value)}
                placeholder={`Nueva contraseña para ${u.nombre} (mínimo 6)`} className={CLASE_INPUT} />
              {errorRestablecer && <p className="text-sm text-danger">{errorRestablecer}</p>}
              <Button type="submit">Restablecer</Button>
            </form>
          )}
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
