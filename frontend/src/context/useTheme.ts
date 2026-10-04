import { createContext, useContext } from 'react'

export type Tema = 'claro' | 'oscuro'

export interface ThemeContextValue {
  tema: Tema
  alternarTema: () => void
}

export const ThemeContext = createContext<ThemeContextValue | undefined>(undefined)

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) throw new Error('useTheme debe usarse dentro de <ThemeProvider>')
  return context
}
