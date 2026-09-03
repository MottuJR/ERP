import { apiClient } from './client';
import type { Insumo, UnidadMedida } from '../types';

export interface InsumoPayload {
  nombre: string;
  unidadMedida: UnidadMedida;
  stockMinimo: number;
  costoUnitario: number;
}

export type ItemTipoStock = 'PRODUCTO' | 'INSUMO';
export type TipoMovimientoStock = 'ENTRADA' | 'SALIDA' | 'AJUSTE' | 'MERMA';

export interface MovimientoManualPayload {
  itemTipo: ItemTipoStock;
  itemId: number;
  tipo: TipoMovimientoStock;
  cantidad: number;
  motivo: string;
}

export async function registrarMovimientoStock(payload: MovimientoManualPayload): Promise<void> {
  await apiClient.post('/api/inventario/movimientos', payload);
}

export async function listarInsumos(): Promise<Insumo[]> {
  const { data } = await apiClient.get<Insumo[]>('/api/inventario/insumos');
  return data;
}

export async function crearInsumo(payload: InsumoPayload): Promise<Insumo> {
  const { data } = await apiClient.post<Insumo>('/api/inventario/insumos', payload);
  return data;
}

export async function actualizarInsumo(id: number, payload: InsumoPayload): Promise<Insumo> {
  const { data } = await apiClient.put<Insumo>(`/api/inventario/insumos/${id}`, payload);
  return data;
}
