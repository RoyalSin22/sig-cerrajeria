import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { extraerMensajeError } from '../api/axiosClient'
import { Button } from '../components/ui/Button'
import { useNegocio } from '../hooks/useNegocio'

export function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [verPassword, setVerPassword] = useState(false)
  const negocio = useNegocio()
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
        <h1 className="font-heading mb-1 text-center text-4xl font-bold text-steel-dark">{negocio.nombre}</h1>
        <p className="mb-8 text-center text-lg text-sub">Escribe tu usuario y tu contraseña</p>

        <form onSubmit={handleSubmit} className="panel-cepillado flex flex-col gap-4 rounded-lg p-6">
          <div>
            <label htmlFor="username" className="mb-1 block text-lg font-semibold text-ink">
              Usuario
            </label>
            <input
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              enterKeyHint="next"
              required
              className="w-full rounded-lg border px-3 text-lg outline-none"
            />
          </div>

          <div>
            <label htmlFor="password" className="mb-1 block text-lg font-semibold text-ink">
              Contraseña
            </label>
            <div className="flex gap-2">
              <input
                id="password"
                type={verPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                autoCapitalize="none"
                autoCorrect="off"
                spellCheck={false}
                enterKeyHint="go"
                required
                className="w-full min-w-0 rounded-lg border px-3 text-lg outline-none"
              />
              {/* Ver lo escrito evita el error mas comun: una letra mal puesta */}
              <Button type="button" variant="secondary" onClick={() => setVerPassword((v) => !v)} className="shrink-0">
                {verPassword ? 'Ocultar' : 'Ver'}
              </Button>
            </div>
          </div>

          {error && <p className="text-base font-semibold text-danger">{error}</p>}

          <Button type="submit" tamano="lg" disabled={cargando} className="w-full text-xl">
            {cargando ? 'Ingresando…' : 'Ingresar'}
          </Button>
        </form>
      </div>
    </div>
  )
}
