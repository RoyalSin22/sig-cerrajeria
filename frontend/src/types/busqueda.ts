// Refleja com.cerrajeria.sig.busqueda.ResultadoBusquedaResponse

import type { ProductoResponse } from './productos'
import type { ClienteResponse } from './clientes'
import type { OrdenTrabajoResponse } from './ordenes'

export interface ResultadoBusquedaResponse {
  productos: ProductoResponse[]
  clientes: ClienteResponse[]
  ordenes: OrdenTrabajoResponse[]
}
