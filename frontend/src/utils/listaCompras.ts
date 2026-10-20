import type { ProductoResponse } from '../types/productos'

/**
 * La lista de lo que hay que ir a comprar donde los distribuidores. Se arma
 * durante la semana y vive en este dispositivo (localStorage): no es un dato
 * del negocio, es un apunte. Al registrar la compra se quita lo comprado.
 */
export interface ItemListaCompras {
  productoId: number
  codigo: string
  nombre: string
  cantidad: number
  stock: number
  precioCompra: number
}

const CLAVE = 'sig.listaCompras'

export function leerLista(): ItemListaCompras[] {
  try {
    const guardada = JSON.parse(localStorage.getItem(CLAVE) ?? '[]')
    return Array.isArray(guardada) ? guardada : []
  } catch {
    return []
  }
}

export function guardarLista(items: ItemListaCompras[]) {
  try {
    localStorage.setItem(CLAVE, JSON.stringify(items))
  } catch {
    // Navegador sin almacenamiento (modo privado): la lista dura mientras la pantalla siga abierta
  }
}

/** Lo que falta para llegar al doble del minimo; al menos 1. */
export function cantidadSugerida(p: ProductoResponse): number {
  return Math.max(1, p.stockMinimo * 2 - p.cantidadStock)
}

export function itemDesdeProducto(p: ProductoResponse): ItemListaCompras {
  return {
    productoId: p.id,
    codigo: p.codigo,
    nombre: p.nombre,
    cantidad: cantidadSugerida(p),
    stock: p.cantidadStock,
    precioCompra: p.precioCompra,
  }
}

/** Texto para mandar por WhatsApp o copiar. */
export function textoLista(items: ItemListaCompras[], fecha: string): string {
  return [`Lista de compras · ${fecha}`, ...items.map((i) => `• ${i.cantidad} × ${i.nombre} (${i.codigo})`)].join('\n')
}
