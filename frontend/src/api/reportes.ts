import { apiClient } from './client';
import type { MargenProducto, ProductoMasVendido, ReporteIngresos, ReporteVentas, StockCritico } from '../types';

export async function obtenerReporteVentas(desde: string, hasta: string): Promise<ReporteVentas> {
  const { data } = await apiClient.get<ReporteVentas>('/api/reportes/ventas', { params: { desde, hasta } });
  return data;
}

export async function obtenerIngresosPorMedioPago(desde: string, hasta: string): Promise<ReporteIngresos> {
  const { data } = await apiClient.get<ReporteIngresos>('/api/reportes/ingresos-por-medio-pago', {
    params: { desde, hasta },
  });
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
