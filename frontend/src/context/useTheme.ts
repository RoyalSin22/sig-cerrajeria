import { createContext, useContext } from 'react'

export type Tema = 'claro' | 'oscuro'

/** Base de la letra: normal 16 px, grande 18 px (por defecto), muy grande 20 px. */
export type TamanoLetra = 'normal' | 'grande' | 'muy-grande'

export interface ThemeContextValue {
  tema: Tema
  alternarTema: () => void
  tamanoLetra: TamanoLetra
  cambiarTamanoLetra: (tamano: TamanoLetra) => void
}

export const ThemeContext = createContext<ThemeContextValue | undefined>(undefined)

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) throw new Error('useTheme debe usarse dentro de <ThemeProvider>')
  return context
}
