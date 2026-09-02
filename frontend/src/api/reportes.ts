import { apiClient } from './client';
import type { MargenProducto, ProductoMasVendido, ReporteVentas, StockCritico } from '../types';

export async function obtenerReporteVentas(desde: string, hasta: string): Promise<ReporteVentas> {
  const { data } = await apiClient.get<ReporteVentas>('/api/reportes/ventas', { params: { desde, hasta } });
  return data;
}

export async function obtenerProductosMasVendidos(
  desde: string,
  hasta: string,
  limite = 10,
): Promise<ProductoMasVendido[]> {
  const { data } = await apiClient.get<ProductoMasVendido[]>('/api/reportes/productos-mas-vendidos', {
    params: { desde, hasta, limite },
  });
  return data;
}

export async function obtenerMargenProductos(): Promise<MargenProducto[]> {
  const { data } = await apiClient.get<MargenProducto[]>('/api/reportes/margen-productos');
  return data;
}

export async function obtenerStockCritico(): Promise<StockCritico> {
  const { data } = await apiClient.get<StockCritico>('/api/reportes/stock-critico');
  return data;
}
