import { apiClient } from './client';
import type { Rol, Usuario } from '../types';

export interface CrearUsuarioPayload {
  nombre: string;
  email: string;
  password: string;
  rol: Rol;
  porcentajeComision: number | null;
}

export interface ActualizarUsuarioPayload {
  nombre: string;
  email: string;
  rol: Rol;
  activo: boolean;
  porcentajeComision: number | null;
  password?: string;
}

export async function listarUsuarios(): Promise<Usuario[]> {
  const { data } = await apiClient.get<Usuario[]>('/api/usuarios');
  return data;
}

export async function crearUsuario(payload: CrearUsuarioPayload): Promise<Usuario> {
  const { data } = await apiClient.post<Usuario>('/api/usuarios', payload);
  return data;
}

export async function actualizarUsuario(id: number, payload: ActualizarUsuarioPayload): Promise<Usuario> {
  const { data } = await apiClient.put<Usuario>(`/api/usuarios/${id}`, payload);
  return data;
}
