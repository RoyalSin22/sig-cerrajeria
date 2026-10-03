import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
}

const estilos: Record<Variant, string> = {
  primary: 'bg-steel-dark text-white hover:opacity-90 disabled:opacity-50',
  secondary: 'border border-line text-ink hover:bg-hair disabled:opacity-50',
  ghost: 'text-steel hover:bg-tint-sky disabled:opacity-50',
  danger: 'bg-danger text-white hover:opacity-90 disabled:opacity-50',
}

export function Button({ variant = 'primary', className = '', ...props }: Props) {
  return (
    <button
      className={`rounded-md px-4 py-2.5 font-medium text-sm transition-colors min-h-11 ${estilos[variant]} ${className}`}
      {...props}
    />
  )
}
