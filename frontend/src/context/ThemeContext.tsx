import { useEffect, useState, type ReactNode } from 'react'
import { ThemeContext, type Tema } from './useTheme'

const CLAVE_STORAGE = 'sig-cerrajeria:tema'

function temaInicial(): Tema {
  const guardado = localStorage.getItem(CLAVE_STORAGE)
  if (guardado === 'claro' || guardado === 'oscuro') return guardado
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'oscuro' : 'claro'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [tema, setTema] = useState<Tema>(temaInicial)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', tema === 'oscuro')
    localStorage.setItem(CLAVE_STORAGE, tema)
  }, [tema])

  function alternarTema() {
    setTema((t) => (t === 'claro' ? 'oscuro' : 'claro'))
  }

  return <ThemeContext.Provider value={{ tema, alternarTema }}>{children}</ThemeContext.Provider>
}
