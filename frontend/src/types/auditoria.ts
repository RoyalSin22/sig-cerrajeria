// Refleja com.cerrajeria.sig.common.audit.dto.AuditLogResponse

export interface AuditLogResponse {
  id: number
  usuario: string
  accion: string
  entidad: string
  entidadId: number
  fechaHora: string
}
