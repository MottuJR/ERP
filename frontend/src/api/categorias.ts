import { apiClient } from './client';

export interface Categoria {
  id: number;
  nombre: string;
}

export async function listarCategorias(): Promise<Categoria[]> {
  const { data } = await apiClient.get<Categoria[]>('/api/categorias');
  return data;
}

export async function crearCategoria(nombre: string): Promise<Categoria> {
  const { data } = await apiClient.post<Categoria>('/api/categorias', { nombre });
  return data;
}
