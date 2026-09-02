import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
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

  if (roles && !hasRole(...roles)) {
    return <Navigate to="/pos" replace />;
  }

  return <>{children}</>;
}
