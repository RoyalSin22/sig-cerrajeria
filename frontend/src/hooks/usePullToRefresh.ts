import { useEffect, useRef, useState } from 'react'

const UMBRAL_PX = 70
const MAX_PX = 120

/**
 * Fase 7 §5.0: gesto de "deslizar para actualizar" en listados moviles.
 * Solo se activa si el scroll ya esta en el tope (no interfiere con el
 * scroll normal de la pagina).
 */
export function usePullToRefresh(onRefresh: () => Promise<void> | void) {
  const [refrescando, setRefrescando] = useState(false)
  const [distancia, setDistancia] = useState(0)
  const inicioY = useRef<number | null>(null)
  const distanciaRef = useRef(0)
  const onRefreshRef = useRef(onRefresh)
  onRefreshRef.current = onRefresh

  useEffect(() => {
    function handleTouchStart(e: TouchEvent) {
      if (window.scrollY <= 0) {
        inicioY.current = e.touches[0].clientY
      }
    }

    function handleTouchMove(e: TouchEvent) {
      if (inicioY.current === null) return
      const delta = e.touches[0].clientY - inicioY.current
      if (delta > 0 && window.scrollY <= 0) {
        const limitado = Math.min(delta, MAX_PX)
        distanciaRef.current = limitado
        setDistancia(limitado)
      }
    }

    async function handleTouchEnd() {
      if (inicioY.current === null) return
      inicioY.current = null
      if (distanciaRef.current > UMBRAL_PX) {
        setRefrescando(true)
        try {
          await onRefreshRef.current()
        } finally {
          setRefrescando(false)
        }
      }
      distanciaRef.current = 0
      setDistancia(0)
    }

    window.addEventListener('touchstart', handleTouchStart, { passive: true })
    window.addEventListener('touchmove', handleTouchMove, { passive: true })
    window.addEventListener('touchend', handleTouchEnd)
    return () => {
      window.removeEventListener('touchstart', handleTouchStart)
      window.removeEventListener('touchmove', handleTouchMove)
      window.removeEventListener('touchend', handleTouchEnd)
    }
  }, [])

  return { refrescando, distancia, umbral: UMBRAL_PX }
}
