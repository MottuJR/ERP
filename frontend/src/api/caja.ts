import axios from 'axios';
import { apiClient } from './client';
import type { Caja, MovimientoCaja, TipoMovimientoCaja } from '../types';

export async function obtenerCajaActual(): Promise<Caja | null> {
  try {
    const { data } = await apiClient.get<Caja>('/api/caja/actual');
    return data;
  } catch (err) {
    if (axios.isAxiosError(err) && err.response?.status === 404) {
      return null;
    }
    throw err;
  }
}

export async function abrirCaja(montoInicial: number): Promise<Caja> {
  const { data } = await apiClient.post<Caja>('/api/caja/abrir', { montoInicial });
  return data;
}

export async function cerrarCaja(id: number, montoFinal: number): Promise<Caja> {
  const { data } = await apiClient.post<Caja>(`/api/caja/${id}/cerrar`, { montoFinal });
  return data;
}

export async function listarMovimientos(id: number): Promise<MovimientoCaja[]> {
  const { data } = await apiClient.get<MovimientoCaja[]>(`/api/caja/${id}/movimientos`);
  return data;
}

export interface MovimientoCajaPayload {
  tipo: TipoMovimientoCaja;
  monto: number;
  concepto: string;
}

export async function registrarMovimiento(id: number, payload: MovimientoCajaPayload): Promise<MovimientoCaja> {
  const { data } = await apiClient.post<MovimientoCaja>(`/api/caja/${id}/movimientos`, payload);
  return data;
}
