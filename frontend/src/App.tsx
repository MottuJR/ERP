import type { ReactNode } from 'react';
import { Navigate, Route, BrowserRouter, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { NAV_ITEMS, primeraRutaPermitida } from './layout/navItems';
import { LoginPage } from './pages/LoginPage';
import { CajaPage } from './pages/CajaPage';
import { HistorialCajasPage } from './pages/HistorialCajasPage';
import { PosPage } from './pages/PosPage';
import { ProductosPage } from './pages/ProductosPage';
import { InsumosPage } from './pages/InsumosPage';
import { RecetasPage } from './pages/RecetasPage';
import { ProduccionPage } from './pages/ProduccionPage';
import { ProveedoresPage } from './pages/ProveedoresPage';
import { ComprasPage } from './pages/ComprasPage';
import { ClientesPage } from './pages/ClientesPage';
import { ReportesPage } from './pages/ReportesPage';
import { ComisionesPage } from './pages/ComisionesPage';
import { AuditoriaPage } from './pages/AuditoriaPage';
import { UsuariosPage } from './pages/UsuariosPage';

function protegida(path: string, element: ReactNode) {
  const roles = NAV_ITEMS.find((item) => item.key === path)?.roles;
  return <ProtectedRoute roles={roles}>{element}</ProtectedRoute>;
}

// Destino de una ruta desconocida o de "/": no puede ser un path fijo porque no todos los
// roles tienen acceso a la misma primera sección (ej. PRODUCCION no ve "/pos").
function RedirectPorDefecto() {
  const { isAuthenticated, hasRole } = useAuth();
  return <Navigate to={isAuthenticated ? primeraRutaPermitida(hasRole) : '/login'} replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/caja" element={protegida('/caja', <CajaPage />)} />
          <Route path="/historial-cajas" element={protegida('/historial-cajas', <HistorialCajasPage />)} />
          <Route path="/pos" element={protegida('/pos', <PosPage />)} />
          <Route path="/productos" element={protegida('/productos', <ProductosPage />)} />
          <Route path="/insumos" element={protegida('/insumos', <InsumosPage />)} />
          <Route path="/recetas" element={protegida('/recetas', <RecetasPage />)} />
          <Route path="/produccion" element={protegida('/produccion', <ProduccionPage />)} />
          <Route path="/proveedores" element={protegida('/proveedores', <ProveedoresPage />)} />
          <Route path="/compras" element={protegida('/compras', <ComprasPage />)} />
          <Route path="/clientes" element={protegida('/clientes', <ClientesPage />)} />
          <Route path="/reportes" element={protegida('/reportes', <ReportesPage />)} />
          <Route path="/comisiones" element={protegida('/comisiones', <ComisionesPage />)} />
          <Route path="/auditoria" element={protegida('/auditoria', <AuditoriaPage />)} />
          <Route path="/usuarios" element={protegida('/usuarios', <UsuariosPage />)} />
          <Route path="*" element={<RedirectPorDefecto />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
