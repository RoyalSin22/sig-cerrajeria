import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Tamano = 'md' | 'lg'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  /** lg: la accion principal de la pantalla (Cobrar, Guardar), mas alta y con letra mas grande. */
  tamano?: Tamano
}

const estilos: Record<Variant, string> = {
  primary: 'bg-steel-dark text-white hover:opacity-90 disabled:opacity-50',
  // Borde de 2px: se ve que es un boton y no un texto
  secondary: 'border-2 border-line bg-card text-ink hover:bg-hair disabled:opacity-50',
  ghost: 'text-steel underline-offset-4 hover:bg-tint-sky hover:underline disabled:opacity-50',
  danger: 'bg-danger text-white hover:opacity-90 disabled:opacity-50',
}

const tamanos: Record<Tamano, string> = {
  // 48 px de alto como minimo: se acierta con el dedo sin precision
  md: 'min-h-12 px-5 text-base',
  lg: 'min-h-14 px-6 text-lg',
}

export function Button({ variant = 'primary', tamano = 'md', className = '', ...props }: Props) {
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-colors ${tamanos[tamano]} ${estilos[variant]} ${className}`}
      {...props}
    />
  )
}
