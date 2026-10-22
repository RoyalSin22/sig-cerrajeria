import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Moon, Sun } from 'lucide-react'
import { usuariosApi } from '../api/usuarios'
import { extraerMensajeError } from '../api/axiosClient'
import { Card } from '../components/ui/Card'
import { negocioApi, type Negocio } from '../api/negocio'
import { useNegocio, actualizarNegocio } from '../hooks/useNegocio'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Campo } from '../components/ui/Campo'
import { useAuth } from '../context/useAuth'
import { useTheme } from '../context/useTheme'
import { useToast } from '../context/useToast'
import type { Rol, UsuarioResponse } from '../types/usuarios'

export function Ajustes() {
  const { usuario } = useAuth()
  const { tema, alternarTema, tamanoLetra, cambiarTamanoLetra } = useTheme()
  const navigate = useNavigate()

  return (
    <div className="mx-auto flex max-w-lg flex-col gap-5">
      <h1 className="font-heading text-3xl font-bold">Ajustes</h1>

      <Card className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium">Sesión</p>
          <p className="text-xs text-sub">{usuario?.nombre} · {usuario?.role === 'ROLE_ADMIN' ? 'Administrador' : 'Empleado'}</p>
        </div>
      </Card>

      <CambiarMiPassword />

      <Card className="flex flex-col gap-3">
        <div>
          <p className="font-medium">Tamaño de la letra</p>
          <p className="text-sm text-sub">Se guarda en este equipo. Elige el que se lea cómodo sin lentes.</p>
        </div>
        <div className="grid grid-cols-3 gap-2" role="radiogroup" aria-label="Tamaño de la letra">
          {([
            ['normal', 'Normal', 'text-base'],
            ['grande', 'Grande', 'text-lg'],
            ['muy-grande', 'Muy grande', 'text-xl'],
          ] as const).map(([valor, etiqueta, clase]) => (
            <button
              key={valor}
              type="button"
              role="radio"
              aria-checked={tamanoLetra === valor}
              onClick={() => cambiarTamanoLetra(valor)}
              className={`flex min-h-16 flex-col items-center justify-center rounded-lg border-2 px-2 py-2 font-semibold ${
                tamanoLetra === valor ? 'border-steel-dark bg-tint-sky text-steel-dark' : 'border-line'
              }`}
            >
              <span className={clase}>Aa</span>
              <span className="text-sm">{etiqueta}</span>
            </button>
          ))}
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

      {usuario?.role === 'ROLE_ADMIN' && <DatosNegocio />}

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
          <Campo etiqueta="Contraseña actual"><input required type="password" autoComplete="current-password" value={actual}
            onChange={(e) => setActual(e.target.value)} className={CLASE_INPUT} /></Campo>
          <Campo etiqueta="Nueva contraseña" ayuda="Mínimo 6 letras o números"><input required type="password" autoComplete="new-password" minLength={6} value={nueva}
            onChange={(e) => setNueva(e.target.value)} className={CLASE_INPUT} /></Campo>
          <Campo etiqueta="Repite la nueva contraseña"><input required type="password" autoComplete="new-password" minLength={6} value={confirmacion}
            onChange={(e) => setConfirmacion(e.target.value)} className={CLASE_INPUT} /></Campo>
          {error && <p className="text-base font-semibold text-danger">{error}</p>}
          <Button type="submit" tamano="lg" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar nueva contraseña'}</Button>
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
              {errorRestablecer && <p className="text-base font-semibold text-danger">{errorRestablecer}</p>}
              <Button type="submit" tamano="lg">Restablecer</Button>
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
      <Campo etiqueta="Nombre completo"><input required value={nombre} onChange={(e) => setNombre(e.target.value)}
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" /></Campo>
      <Campo etiqueta="Usuario" ayuda="Con el que va a entrar, sin espacios"><input required value={username} onChange={(e) => setUsername(e.target.value)}
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" /></Campo>
      <Campo etiqueta="Contraseña" ayuda="Mínimo 6 letras o números"><input required type="password" minLength={6} value={password} onChange={(e) => setPassword(e.target.value)}
       
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel" /></Campo>
      <select value={role} onChange={(e) => setRole(e.target.value as Rol)}
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-steel">
        <option value="ROLE_EMPLOYEE">Empleado</option>
        <option value="ROLE_ADMIN">Administrador</option>
      </select>
      {error && <p className="text-base font-semibold text-danger">{error}</p>}
      <Button type="submit" tamano="lg" disabled={guardando}>{guardando ? 'Creando…' : 'Crear usuario'}</Button>
    </form>
  )
}

/**
 * Nombre, telefono y direccion del negocio: salen en el menu, en la pantalla
 * de ingreso y en los mensajes de WhatsApp a los clientes.
 */
function DatosNegocio() {
  const actual = useNegocio()
  const { notificar } = useToast()
  const [datos, setDatos] = useState<Negocio | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)
  const valores = datos ?? actual

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setGuardando(true)
    try {
      const guardado = await negocioApi.guardar(valores)
      actualizarNegocio(guardado)
      setDatos(null)
      notificar('Datos del negocio guardados')
    } catch (err) {
      setError(extraerMensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  const cambiar = (campo: keyof Negocio) => (e: ChangeEvent<HTMLInputElement>) =>
    setDatos({ ...valores, [campo]: e.target.value })

  return (
    <Card>
      <form onSubmit={guardar} className="flex flex-col gap-3">
        <div>
          <p className="font-medium">Datos del negocio</p>
          <p className="text-sm text-sub">Salen en el menú y en los mensajes de WhatsApp a los clientes.</p>
        </div>
        <Campo etiqueta="Nombre">
          <input required value={valores.nombre} onChange={cambiar('nombre')} maxLength={100} className="rounded-lg border px-3 outline-none" />
        </Campo>
        <Campo etiqueta="Teléfono / WhatsApp">
          <input required type="tel" inputMode="tel" value={valores.telefono} onChange={cambiar('telefono')} className="rounded-lg border px-3 outline-none" />
        </Campo>
        <Campo etiqueta="Dirección">
          <input required value={valores.direccion} onChange={cambiar('direccion')} maxLength={200} className="rounded-lg border px-3 outline-none" />
        </Campo>
        {error && <p className="text-base font-semibold text-danger">{error}</p>}
        <Button type="submit" disabled={guardando || datos === null}>{guardando ? 'Guardando…' : 'Guardar'}</Button>
      </form>
    </Card>
  )
}
