import { axiosClient } from './axiosClient'
import type {
  ReporteVentasResponse, ProductoMasVendidoResponse, ReporteInventarioResponse,
  ReporteServiciosResponse, ReporteClientesResponse,
} from '../types/reportes'

export const reportesApi = {
  ventas: (desde: string, hasta: string) =>
    axiosClient.get<ReporteVentasResponse>('/reportes/ventas', { params: { desde, hasta } }).then(r => r.data),
  productosMasVendidos: (desde: string, hasta: string) =>
    axiosClient.get<ProductoMasVendidoResponse[]>('/reportes/productos-mas-vendidos', { params: { desde, hasta } }).then(r => r.data),
  inventario: () => axiosClient.get<ReporteInventarioResponse>('/reportes/inventario').then(r => r.data),
  servicios: (desde: string, hasta: string) =>
    axiosClient.get<ReporteServiciosResponse>('/reportes/servicios', { params: { desde, hasta } }).then(r => r.data),
  clientes: (desde: string, hasta: string, limite = 20) =>
    axiosClient.get<ReporteClientesResponse>('/reportes/clientes', { params: { desde, hasta, limite } }).then(r => r.data),
}
