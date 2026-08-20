import { apiClient } from './client';
import type { Producto } from '../types';

export async function listarProductos(): Promise<Producto[]> {
  const { data } = await apiClient.get<Producto[]>('/api/productos');
  return data;
}
