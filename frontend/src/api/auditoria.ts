import { apiClient } from './client';

export interface RegistroAuditoria {
  id: number;
  usuarioId: number | null;
  usuarioNombre: string | null;
  entidad: string;
  entidadId: number | null;
  accion: string;
  fecha: string;
  detalle: string | null;
}

export async function listarAuditoria(entidad?: string): Promise<RegistroAuditoria[]> {
  const { data } = await apiClient.get<RegistroAuditoria[]>('/api/auditoria', {
    params: entidad ? { entidad } : undefined,
  });
  return data;
}
