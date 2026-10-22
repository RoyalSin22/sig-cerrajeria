import { useEffect, useState, type ReactNode } from 'react'
import { ThemeContext, type Tema, type TamanoLetra } from './useTheme'

const CLAVE_STORAGE = 'sig-cerrajeria:tema'
const CLAVE_LETRA = 'sig-cerrajeria:letra'

function leer(clave: string): string | null {
  try {
    return localStorage.getItem(clave)
  } catch {
    return null
  }
}

function guardar(clave: string, valor: string) {
  try {
    localStorage.setItem(clave, valor)
  } catch {
    // Sin almacenamiento (modo privado): el ajuste dura mientras la pestana siga abierta
  }
}

function temaInicial(): Tema {
  const guardado = leer(CLAVE_STORAGE)
  if (guardado === 'claro' || guardado === 'oscuro') return guardado
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'oscuro' : 'claro'
}

function letraInicial(): TamanoLetra {
  const guardado = leer(CLAVE_LETRA)
  return guardado === 'normal' || guardado === 'muy-grande' ? guardado : 'grande'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [tema, setTema] = useState<Tema>(temaInicial)
  const [tamanoLetra, cambiarTamanoLetra] = useState<TamanoLetra>(letraInicial)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', tema === 'oscuro')
    guardar(CLAVE_STORAGE, tema)
  }, [tema])

  // "grande" es la base del CSS (html sin clase); las otras dos la cambian
  useEffect(() => {
    const html = document.documentElement
    html.classList.toggle('letra-normal', tamanoLetra === 'normal')
    html.classList.toggle('letra-muy-grande', tamanoLetra === 'muy-grande')
    guardar(CLAVE_LETRA, tamanoLetra)
  }, [tamanoLetra])

  function alternarTema() {
    setTema((t) => (t === 'claro' ? 'oscuro' : 'claro'))
  }

  return (
    <ThemeContext.Provider value={{ tema, alternarTema, tamanoLetra, cambiarTamanoLetra }}>
      {children}
    </ThemeContext.Provider>
  )
}
