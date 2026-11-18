import { useEffect, useState } from 'react'
import { negocioApi, type Negocio } from '../api/negocio'

// Mientras carga (o si falla), lo de la tarjeta de presentacion
const NEGOCIO_POR_DEFECTO: Negocio = {
  nombre: 'Cerrajería Barrera',
  telefono: '982616709',
  direccion: 'Av. Alfonso Ugarte N° 115, frente al Mercado N° 2, Surco',
}

let cache: Negocio | null = null
let pedido: Promise<Negocio> | null = null
const oyentes = new Set<(n: Negocio) => void>()

/** Datos del negocio (nombre, telefono, direccion), pedidos una sola vez para toda la app. */
export function useNegocio(): Negocio {
  const [negocio, setNegocio] = useState<Negocio>(cache ?? NEGOCIO_POR_DEFECTO)
  useEffect(() => {
    oyentes.add(setNegocio)
    if (!cache) {
      pedido ??= negocioApi.obtener().then((n) => (cache = n)).catch(() => NEGOCIO_POR_DEFECTO)
      pedido.then(setNegocio)
    }
    return () => {
      oyentes.delete(setNegocio)
    }
  }, [])
  return negocio
}

/** Despues de guardarlos en Ajustes: toda la app ve los datos nuevos. */
export function actualizarNegocio(n: Negocio) {
  cache = n
  oyentes.forEach((f) => f(n))
}
