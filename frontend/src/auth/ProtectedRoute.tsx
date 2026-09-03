import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { primeraRutaPermitida } from '../layout/navItems';
import type { Rol } from '../types';

interface ProtectedRouteProps {
  children: ReactNode;
  /** Si se pasa, además de estar autenticado el usuario tiene que tener uno de estos roles. */
  roles?: Rol[];
}

export function ProtectedRoute({ children, roles }: ProtectedRouteProps) {
  const { isAuthenticated, hasRole } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // No todos los roles pueden ver "/pos" (ej. PRODUCCION no vende), así que el destino de
  // redirect tiene que ser la primera sección a la que el usuario SÍ tenga acceso.
  if (roles && !hasRole(...roles)) {
    return <Navigate to={primeraRutaPermitida(hasRole)} replace />;
  }

  return <>{children}</>;
}
