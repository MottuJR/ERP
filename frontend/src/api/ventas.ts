import { apiClient } from './client';
import type { EscaneoResponse, MedioPago, VentaResponse } from '../types';

export async function escanear(codigo: string): Promise<EscaneoResponse> {
  const { data } = await apiClient.get<EscaneoResponse>('/api/ventas/escanear', {
    params: { codigo },
  });
  return data;
}

export interface ItemVentaPayload {
  codigoEscaneado?: string;
  productoId?: number;
  cantidad?: number;
}

export interface ConfirmarVentaPayload {
  clienteId?: number | null;
  cajaId?: number | null;
  medioPago: MedioPago;
  items: ItemVentaPayload[];
}

export async function confirmarVenta(payload: ConfirmarVentaPayload): Promise<VentaResponse> {
  const { data } = await apiClient.post<VentaResponse>('/api/ventas', payload);
  return data;
}
