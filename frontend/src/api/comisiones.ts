import { apiClient } from './client';
import type { ComisionProduccion, ComisionVendedor } from '../types';

export async function obtenerComisionesVendedores(desde: string, hasta: string): Promise<ComisionVendedor[]> {
  const { data } = await apiClient.get<ComisionVendedor[]>('/api/comisiones/vendedores', {
    params: { desde, hasta },
  });
  return data;
}

export async function obtenerComisionesProduccion(desde: string, hasta: string): Promise<ComisionProduccion[]> {
  const { data } = await apiClient.get<ComisionProduccion[]>('/api/comisiones/produccion', {
    params: { desde, hasta },
  });
  return data;
}
