import type { ReactNode } from 'react'

interface Props {
  etiqueta: string
  /** Texto chico debajo de la etiqueta: un ejemplo o para que sirve. */
  ayuda?: string
  opcional?: boolean
  className?: string
  children: ReactNode
}

/**
 * Campo de formulario con la etiqueta siempre visible arriba. El texto gris de
 * ejemplo (placeholder) desaparece al escribir; la etiqueta no, asi nunca se
 * pierde que se esta llenando.
 */
export function Campo({ etiqueta, ayuda, opcional, className = '', children }: Props) {
  return (
    <label className={`flex flex-col gap-1 ${className}`}>
      <span className="text-base font-semibold">
        {etiqueta}
        {opcional && <span className="font-normal text-sub"> (si hay)</span>}
      </span>
      {ayuda && <span className="text-sm text-sub">{ayuda}</span>}
      {children}
    </label>
  )
}
