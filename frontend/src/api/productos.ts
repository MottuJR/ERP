import { apiClient } from './client';
import type { Producto, TipoProducto, UnidadMedida } from '../types';

export interface ProductoPayload {
  nombre: string;
  categoriaId: number;
  tipo: TipoProducto;
  seVendePorPeso: boolean;
  precioVenta: number;
  unidadMedida: UnidadMedida;
  codigoBarras?: string | null;
  codigoPLU?: string | null;
  stockMinimo: number;
  activo?: boolean;
}

export async function listarProductos(): Promise<Producto[]> {
  const { data } = await apiClient.get<Producto[]>('/api/productos');
  return data;
}

export async function crearProducto(payload: ProductoPayload): Promise<Producto> {
  const { data } = await apiClient.post<Producto>('/api/productos', payload);
  return data;
}

export async function actualizarProducto(id: number, payload: ProductoPayload): Promise<Producto> {
  const { data } = await apiClient.put<Producto>(`/api/productos/${id}`, payload);
  return data;
}

export async function desactivarProducto(id: number): Promise<void> {
  await apiClient.delete(`/api/productos/${id}`);
}
