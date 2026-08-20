import axios from 'axios';
import type { ApiErrorBody } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const apiClient = axios.create({ baseURL: API_BASE_URL });

const TOKEN_KEY = 'erp_token';

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && window.location.pathname !== '/login') {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem('erp_usuario');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

export function mensajeDeError(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiErrorBody>(error) && error.response?.data?.error) {
    return error.response.data.error;
  }
  return fallback;
}
