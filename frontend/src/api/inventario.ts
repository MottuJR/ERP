import { apiClient } from './client';
import type { Insumo } from '../types';

export async function listarInsumos(): Promise<Insumo[]> {
  const { data } = await apiClient.get<Insumo[]>('/api/inventario/insumos');
  return data;
}
