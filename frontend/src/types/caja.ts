// Refleja com.cerrajeria.sig.caja.dto.*

export type EstadoCaja = 'ABIERTA' | 'CERRADA'

export interface CajaDiariaResponse {
  id: number
  usuario: string
  fechaApertura: string
  fechaCierre: string | null
  montoApertura: number
  montoCierre: number | null
  montoIngresos: number
  montoEgresos: number
  saldoEsperado: number
  estado: EstadoCaja
}

export interface AperturaCajaRequest {
  montoApertura: number
}

export interface CierreCajaRequest {
  montoContado: number
}

export interface CierreCajaResponse {
  caja: CajaDiariaResponse
  montoContado: number
  diferencia: number
}
