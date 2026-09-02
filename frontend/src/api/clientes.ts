import { apiClient } from './client';
import type { Cliente, MedioPago, PagoCliente, SaldoCliente } from '../types';

export interface ClientePayload {
  nombre: string;
  telefono?: string | null;
  tieneCuentaCorriente: boolean;
}

export async function listarClientes(): Promise<Cliente[]> {
  const { data } = await apiClient.get<Cliente[]>('/api/clientes');
  return data;
}

export async function crearCliente(payload: ClientePayload): Promise<Cliente> {
  const { data } = await apiClient.post<Cliente>('/api/clientes', payload);
  return data;
}

export async function actualizarCliente(id: number, payload: ClientePayload): Promise<Cliente> {
  const { data } = await apiClient.put<Cliente>(`/api/clientes/${id}`, payload);
  return data;
}

export async function obtenerSaldoCliente(id: number): Promise<SaldoCliente> {
  const { data } = await apiClient.get<SaldoCliente>(`/api/clientes/${id}/saldo`);
  return data;
}

export async function listarPagosCliente(id: number): Promise<PagoCliente[]> {
  const { data } = await apiClient.get<PagoCliente[]>(`/api/clientes/${id}/pagos`);
  return data;
}

export async function registrarPagoCliente(
  id: number,
  monto: number,
  medioPago: MedioPago,
): Promise<PagoCliente> {
  const { data } = await apiClient.post<PagoCliente>(`/api/clientes/${id}/pagos`, { monto, medioPago });
  return data;
}
