import { createContext, useContext, useState, type ReactNode } from 'react';
import { login as loginRequest } from '../api/auth';
import type { Usuario } from '../types';

interface AuthContextValue {
  usuario: Usuario | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (...roles: Usuario['rol'][]) => boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const TOKEN_KEY = 'erp_token';
const USUARIO_KEY = 'erp_usuario';

function usuarioGuardado(): Usuario | null {
  const raw = localStorage.getItem(USUARIO_KEY);
  return raw ? (JSON.parse(raw) as Usuario) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(usuarioGuardado);
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));

  async function login(email: string, password: string) {
    const response = await loginRequest(email, password);
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USUARIO_KEY, JSON.stringify(response.usuario));
    setToken(response.token);
    setUsuario(response.usuario);
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USUARIO_KEY);
    setToken(null);
    setUsuario(null);
  }

  function hasRole(...roles: Usuario['rol'][]) {
    return usuario !== null && roles.includes(usuario.rol);
  }

  return (
    <AuthContext.Provider value={{ usuario, isAuthenticated: !!token, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  }
  return context;
}
