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
  /** Neto cobrado por cada metodo digital: no esta en el cajon. */
  montoYape: number
  montoPlin: number
  montoTarjeta: number
  /** Total de la caja, todos los metodos. */
  saldoEsperado: number
  /** Lo que deberia haber en el cajon; contra esto se cuadra el cierre. */
  efectivoEsperado: number
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
