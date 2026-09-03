import { apiClient } from './client';
import type { OrdenProduccion, Receta } from '../types';

export interface RecetaItemPayload {
  insumoId: number;
  cantidad: number;
}

export async function obtenerReceta(productoId: number): Promise<Receta> {
  const { data } = await apiClient.get<Receta>(`/api/produccion/recetas/${productoId}`);
  return data;
}

export async function crearReceta(
  productoId: number,
  rendimiento: number,
  items: RecetaItemPayload[],
): Promise<Receta> {
  const { data } = await apiClient.post<Receta>('/api/produccion/recetas', { productoId, rendimiento, items });
  return data;
}

export async function actualizarReceta(
  productoId: number,
  rendimiento: number,
  items: RecetaItemPayload[],
): Promise<Receta> {
  const { data } = await apiClient.put<Receta>(`/api/produccion/recetas/${productoId}`, { rendimiento, items });
  return data;
}

export async function confirmarOrdenProduccion(productoId: number, cantidad: number): Promise<OrdenProduccion> {
  const { data } = await apiClient.post<OrdenProduccion>('/api/produccion/ordenes', { productoId, cantidad });
  return data;
}
