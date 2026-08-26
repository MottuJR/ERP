import { apiClient } from './client';
import type { Compra, Proveedor } from '../types';

export interface ProveedorPayload {
  nombre: string;
  contacto?: string | null;
  telefono?: string | null;
  email?: string | null;
}

export async function listarProveedores(): Promise<Proveedor[]> {
  const { data } = await apiClient.get<Proveedor[]>('/api/proveedores');
  return data;
}

export async function crearProveedor(payload: ProveedorPayload): Promise<Proveedor> {
  const { data } = await apiClient.post<Proveedor>('/api/proveedores', payload);
  return data;
}

export async function actualizarProveedor(id: number, payload: ProveedorPayload): Promise<Proveedor> {
  const { data } = await apiClient.put<Proveedor>(`/api/proveedores/${id}`, payload);
  return data;
}

export interface ItemCompraPayload {
  insumoId: number;
  cantidad: number;
  costoUnitario: number;
}

export async function confirmarCompra(proveedorId: number, items: ItemCompraPayload[]): Promise<Compra> {
  const { data } = await apiClient.post<Compra>('/api/compras', { proveedorId, items });
  return data;
}
