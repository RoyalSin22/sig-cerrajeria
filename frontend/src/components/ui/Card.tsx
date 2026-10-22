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
  className?: string
}

/** Cifra grande estilo dashboard (Fase 7 §5.0): se lee de un vistazo desde el mostrador. */
export function StatCard({ label, value, hint, tint, className = '' }: StatCardProps) {
  const tintClass = tint ? `bg-tint-${tint}` : ''
  // Valores largos (montos) escalan con el ancho de la tarjeta (cqw) para caber en
  // una linea; los cortos (contadores) conservan la cifra grande.
  const esLargo = (typeof value === 'string' || typeof value === 'number') && String(value).length > 6
  // Los montos, grandes como los contadores pero sin desbordar la tarjeta
  const tamano = esLargo ? 'text-[clamp(1.5rem,14cqw,2.25rem)]' : 'text-4xl'
  return (
    <Card className={`@container flex min-w-0 flex-col gap-1 ${tintClass} ${className}`}>
      <span className="text-sm font-semibold text-sub">{label}</span>
      <span className={`whitespace-nowrap font-heading ${tamano} font-bold leading-none text-steel-dark`}>{value}</span>
      {hint && <span className="text-sm text-sub">{hint}</span>}
    </Card>
  )
}
