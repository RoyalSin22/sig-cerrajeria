import { axiosClient } from './axiosClient'
import type { Pagina } from '../types/pagina'
import type {
  ProductoResponse, ProductoCreateRequest, ProductoUpdateRequest,
  AjusteStockRequest, MovimientoInventarioResponse, TipoProducto, ImportacionResultado,
} from '../types/productos'

export const productosApi = {
  listar: (page: number, tipoProducto?: TipoProducto) =>
    axiosClient
      .get<Pagina<ProductoResponse>>('/productos', { params: { page, size: 20, tipoProducto } })
      .then(r => r.data),
  buscar: (q: string) => axiosClient.get<ProductoResponse[]>('/productos/buscar', { params: { q } }).then(r => r.data),
  obtener: (id: number) => axiosClient.get<ProductoResponse>(`/productos/${id}`).then(r => r.data),
  stockBajo: () => axiosClient.get<ProductoResponse[]>('/productos/stock-bajo').then(r => r.data),
  crear: (body: ProductoCreateRequest) => axiosClient.post<ProductoResponse>('/productos', body).then(r => r.data),
  actualizar: (id: number, body: ProductoUpdateRequest) =>
    axiosClient.put<ProductoResponse>(`/productos/${id}`, body).then(r => r.data),
  eliminar: (id: number) => axiosClient.delete(`/productos/${id}`),
  kardex: (id: number) => axiosClient.get<MovimientoInventarioResponse[]>(`/productos/${id}/kardex`).then(r => r.data),
  ajustarStock: (id: number, body: AjusteStockRequest) =>
    axiosClient.post<ProductoResponse>(`/productos/${id}/ajustar-stock`, body).then(r => r.data),
  subirImagen: (id: number, archivo: File) => {
    const formData = new FormData()
    formData.append('archivo', archivo)
    // No se fija Content-Type a mano: el navegador arma el boundary multipart correcto.
    return axiosClient
      .post<ProductoResponse>(`/productos/${id}/imagen`, formData)
      .then(r => r.data)
  },
  /** Con confirmar=false solo revisa el archivo; con true crea los productos si no hay errores. */
  importar: (archivo: File, confirmar: boolean) => {
    const formData = new FormData()
    formData.append('archivo', archivo)
    return axiosClient
      .post<ImportacionResultado>('/productos/importar', formData, { params: { confirmar } })
      .then(r => r.data)
  },
}
