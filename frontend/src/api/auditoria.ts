import { axiosClient } from './axiosClient'
import type { Pagina } from '../types/pagina'
import type { AuditLogResponse } from '../types/auditoria'

export const auditoriaApi = {
  listar: (page: number) =>
    axiosClient.get<Pagina<AuditLogResponse>>('/auditoria', { params: { page, size: 20 } }).then(r => r.data),
}
