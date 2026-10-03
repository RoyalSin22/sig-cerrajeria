import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Bot, Send, X } from 'lucide-react'
import { asistenteApi } from '../api/asistente'
import { extraerMensajeError } from '../api/axiosClient'
import type { TurnoChat } from '../types/asistente'

interface Mensaje {
  rol: 'user' | 'assistant'
  texto: string
}

const SUGERENCIAS = [
  '¿Cuántas llaves KW1 quedan?',
  'Muéstrame los trabajos pendientes',
  'Registra una venta de 2 llaves Cantol',
]

/**
 * Chat con el asistente de IA (Fase 12), disponible en toda la app vía un
 * botón flotante. Reusa el mismo intérprete y las mismas herramientas que
 * el canal de WhatsApp (ver com.cerrajeria.sig.asistente.AsistenteIAService).
 */
export function AsistenteChat() {
  const [abierto, setAbierto] = useState(false)
  const [mensajes, setMensajes] = useState<Mensaje[]>([])
  const [entrada, setEntrada] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const finRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    finRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [mensajes, abierto])

  async function enviar(texto: string) {
    if (!texto.trim() || enviando) return

    const historial: TurnoChat[] = mensajes.map((m) => ({ rol: m.rol, texto: m.texto }))
    setMensajes((prev) => [...prev, { rol: 'user', texto }])
    setEntrada('')
    setEnviando(true)
    setError(null)

    try {
      const res = await asistenteApi.enviarMensaje({ mensaje: texto, historial })
      setMensajes((prev) => [...prev, { rol: 'assistant', texto: res.respuesta }])
    } catch (err) {
      setError(extraerMensajeError(err, 'No se pudo contactar al asistente'))
    } finally {
      setEnviando(false)
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    enviar(entrada)
  }

  return (
    <>
      {/* Botón flotante */}
      <button
        onClick={() => setAbierto((v) => !v)}
        aria-label={abierto ? 'Cerrar asistente' : 'Abrir asistente'}
        className="fixed bottom-20 right-4 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-steel-dark text-white shadow-lg transition-transform hover:scale-105 md:bottom-6"
      >
        {abierto ? <X size={22} /> : <Bot size={22} />}
      </button>

      {/* Panel de chat */}
      {abierto && (
        <div className="panel-cepillado fixed inset-x-3 bottom-36 z-40 flex h-[65vh] flex-col rounded-2xl shadow-xl md:inset-x-auto md:right-4 md:bottom-24 md:h-[32rem] md:w-96">
          <div className="flex items-center gap-2 border-b border-hair px-4 py-3">
            <Bot size={18} className="text-steel-dark" />
            <span className="font-heading font-semibold">Asistente</span>
          </div>

          <div className="flex-1 overflow-y-auto px-4 py-3">
            {mensajes.length === 0 ? (
              <div className="flex h-full flex-col items-center justify-center gap-3 text-center">
                <p className="text-sm text-sub">Pregúntame sobre stock, ventas, órdenes o servicios a domicilio.</p>
                <div className="flex flex-col gap-2">
                  {SUGERENCIAS.map((s) => (
                    <button
                      key={s}
                      onClick={() => enviar(s)}
                      className="rounded-full border border-line px-3 py-1.5 text-xs text-sub hover:border-steel hover:text-steel"
                    >
                      {s}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="flex flex-col gap-3">
                {mensajes.map((m, i) => (
                  <div key={i} className={`flex ${m.rol === 'user' ? 'justify-end' : 'justify-start'}`}>
                    <div
                      className={`max-w-[80%] whitespace-pre-line rounded-2xl px-3 py-2 text-sm ${
                        m.rol === 'user' ? 'bg-steel-dark text-white' : 'bg-hair text-ink'
                      }`}
                    >
                      {m.texto}
                    </div>
                  </div>
                ))}
                {enviando && (
                  <div className="flex justify-start">
                    <div className="rounded-2xl bg-hair px-3 py-2 text-sm text-sub">Escribiendo…</div>
                  </div>
                )}
                <div ref={finRef} />
              </div>
            )}
          </div>

          {error && <p className="px-4 pb-1 text-xs text-danger">{error}</p>}

          <form onSubmit={handleSubmit} className="flex items-center gap-2 border-t border-hair p-3">
            <input
              value={entrada}
              onChange={(e) => setEntrada(e.target.value)}
              placeholder="Escribe tu mensaje…"
              className="flex-1 rounded-full border border-line bg-paper px-4 py-2 text-sm outline-none focus:border-steel"
            />
            <button
              type="submit"
              disabled={enviando || !entrada.trim()}
              aria-label="Enviar"
              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-steel-dark text-white disabled:opacity-40"
            >
              <Send size={16} />
            </button>
          </form>
        </div>
      )}
    </>
  )
}
