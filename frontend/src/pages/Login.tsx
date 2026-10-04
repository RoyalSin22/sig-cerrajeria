import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { extraerMensajeError } from '../api/axiosClient'
import { Button } from '../components/ui/Button'

export function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setCargando(true)
    try {
      await login({ username, password })
      navigate('/', { replace: true })
    } catch (err) {
      setError(extraerMensajeError(err, 'Usuario o contraseña incorrectos'))
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4">
      <div className="w-full max-w-sm">
        <h1 className="font-heading mb-1 text-center text-3xl font-bold tracking-wide text-steel-dark">
          SIG-CERRAJERÍA
        </h1>
        <p className="mb-8 text-center text-sm text-sub">Sistema Integral de Gestión</p>

        <form onSubmit={handleSubmit} className="panel-cepillado flex flex-col gap-4 rounded-lg p-6">
          <div>
            <label htmlFor="username" className="mb-1 block text-sm font-medium text-ink">
              Usuario
            </label>
            <input
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
              className="w-full rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel"
            />
          </div>

          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium text-ink">
              Contraseña
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
              className="w-full rounded-md border border-line bg-paper px-3 py-2.5 text-sm outline-none focus:border-steel"
            />
          </div>

          {error && <p className="text-sm text-danger">{error}</p>}

          <Button type="submit" disabled={cargando} className="w-full">
            {cargando ? 'Ingresando…' : 'Ingresar'}
          </Button>
        </form>
      </div>
    </div>
  )
}
