// Refleja com.cerrajeria.sig.common.audit.dto.AuditLogResponse

export interface AuditLogResponse {
  id: number
  usuario: string
  accion: string
  entidad: string
  entidadId: number
  /** null en registros anteriores a V11 */
  detalle: DetalleAuditoria | null
  fechaHora: string
}

export interface DetalleAuditoria {
  descripcion: string
  motivo?: string
  /** campo → valores anterior y nuevo (como texto; montos con 2 decimales) */
  cambios?: Record<string, { antes: string | null; despues: string | null }>
}
