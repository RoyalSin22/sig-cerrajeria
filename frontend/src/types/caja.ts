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
  /** Contado - esperado al cerrar; negativo = falto dinero. null si sigue abierta o es anterior a V11. */
  diferenciaCierre: number | null
  observacionCierre: string | null
}

export interface AperturaCajaRequest {
  montoApertura: number
}

export interface CierreCajaRequest {
  montoContado: number
  /** Obligatoria si el contado no coincide con el efectivo esperado. */
  observacion?: string
}

export interface CierreCajaResponse {
  caja: CajaDiariaResponse
  montoContado: number
  diferencia: number
}
