import { axiosClient } from './axiosClient'
import type { ReporteExcel, ResultadoVentasAnteriores, ResumenVentasAnteriores } from '../types/analisis'

/** Datos para Power BI e historial de ventas anteriores (solo administrador). */
export const analisisApi = {
  /** Excel con una tabla por tema; siempre se llama igual para reemplazarlo en Power BI. */
  powerBi: (desde: string, hasta: string) =>
    axiosClient.get<Blob>('/analisis/power-bi', { params: { desde, hasta }, responseType: 'blob' }).then(r => r.data),
  /** Reporte en Excel con tablas y filtros: resumen, caja, compras (con lista de compras) o taller (con domicilio). */
  reporteExcel: (reporte: ReporteExcel, desde: string, hasta: string) =>
    axiosClient.get<Blob>(`/analisis/reportes/${reporte}`, { params: { desde, hasta }, responseType: 'blob' }).then(r => r.data),
  resumenVentasAnteriores: () =>
    axiosClient.get<ResumenVentasAnteriores>('/analisis/ventas-anteriores').then(r => r.data),
  /** Con confirmar=false solo revisa el archivo; con true las guarda si no hay errores. */
  cargarVentasAnteriores: (archivo: File, confirmar: boolean) => {
    const formData = new FormData()
    formData.append('archivo', archivo)
    return axiosClient
      .post<ResultadoVentasAnteriores>('/analisis/ventas-anteriores', formData, { params: { confirmar } })
      .then(r => r.data)
  },
  borrarVentasAnteriores: () =>
    axiosClient.delete<{ borradas: number }>('/analisis/ventas-anteriores').then(r => r.data),
}
