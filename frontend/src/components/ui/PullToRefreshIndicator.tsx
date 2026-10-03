import { RefreshCw } from 'lucide-react'

interface Props {
  distancia: number
  refrescando: boolean
  umbral: number
}

export function PullToRefreshIndicator({ distancia, refrescando, umbral }: Props) {
  if (distancia === 0 && !refrescando) return null
  const listo = distancia >= umbral

  return (
    <div
      className="flex items-center justify-center overflow-hidden text-sub transition-[height]"
      style={{ height: refrescando ? 40 : Math.min(distancia, 60) }}
    >
      <RefreshCw
        size={18}
        className={refrescando ? 'animate-spin text-steel' : listo ? 'text-steel' : ''}
        style={!refrescando ? { transform: `rotate(${distancia * 3}deg)` } : undefined}
      />
    </div>
  )
}
