import type { HTMLAttributes, ReactNode } from 'react'

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  variant?: 'cepillado' | 'tecnico'
}

/** Fase 7 §3.1.1: dos tratamientos de tarjeta; "cepillado" es el estandar elegido. */
export function Card({ variant = 'cepillado', className = '', ...props }: CardProps) {
  const base = variant === 'cepillado' ? 'panel-cepillado' : 'plano-tecnico'
  return <div className={`rounded-lg p-4 ${base} ${className}`} {...props} />
}

interface StatCardProps {
  label: string
  value: ReactNode
  hint?: string
  tint?: 'sky' | 'mint' | 'lilac' | 'peach'
}

/** Cifra grande estilo dashboard (Fase 7 §5.0): se lee de un vistazo desde el mostrador. */
export function StatCard({ label, value, hint, tint }: StatCardProps) {
  const tintClass = tint ? `bg-tint-${tint}` : ''
  return (
    <Card className={`flex flex-col gap-1 ${tintClass}`}>
      <span className="text-xs uppercase tracking-wide text-sub">{label}</span>
      <span className="font-heading text-4xl font-bold leading-none text-steel-dark">{value}</span>
      {hint && <span className="text-xs text-sub">{hint}</span>}
    </Card>
  )
}
