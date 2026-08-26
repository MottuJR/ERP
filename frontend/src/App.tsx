import type { ReactNode } from 'react';
import { Navigate, Route, BrowserRouter, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { PosPage } from './pages/PosPage';
import { RecetasPage } from './pages/RecetasPage';
import { ProduccionPage } from './pages/ProduccionPage';
import { ProveedoresPage } from './pages/ProveedoresPage';
import { ComprasPage } from './pages/ComprasPage';

function protegida(element: ReactNode) {
  return <ProtectedRoute>{element}</ProtectedRoute>;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/pos" element={protegida(<PosPage />)} />
          <Route path="/recetas" element={protegida(<RecetasPage />)} />
          <Route path="/produccion" element={protegida(<ProduccionPage />)} />
          <Route path="/proveedores" element={protegida(<ProveedoresPage />)} />
          <Route path="/compras" element={protegida(<ComprasPage />)} />
          <Route path="*" element={<Navigate to="/pos" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
