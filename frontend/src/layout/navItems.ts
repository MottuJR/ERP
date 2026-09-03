import type { Rol } from '../types';

export interface NavItem {
  key: string;
  label: string;
  roles: Rol[];
}

// Única fuente de verdad para qué roles pueden ver cada sección: la usan tanto el menú
// (para no mostrar links a los que no tienen acceso) como las rutas (para bloquear el acceso
// directo por URL) — así no hay forma de que queden desincronizados.
export const NAV_ITEMS: NavItem[] = [
  { key: '/caja', label: 'Caja', roles: ['DUENO', 'ENCARGADO', 'VENDEDOR'] },
  { key: '/historial-cajas', label: 'Historial de cajas', roles: ['DUENO', 'ENCARGADO'] },
  { key: '/pos', label: 'Venta', roles: ['DUENO', 'ENCARGADO', 'VENDEDOR'] },
  { key: '/productos', label: 'Productos', roles: ['DUENO', 'ENCARGADO'] },
  { key: '/insumos', label: 'Insumos', roles: ['DUENO', 'ENCARGADO', 'PRODUCCION'] },
  { key: '/recetas', label: 'Recetas', roles: ['DUENO', 'ENCARGADO', 'PRODUCCION'] },
  { key: '/produccion', label: 'Producción', roles: ['DUENO', 'ENCARGADO', 'PRODUCCION'] },
  { key: '/proveedores', label: 'Proveedores', roles: ['DUENO', 'ENCARGADO'] },
  { key: '/compras', label: 'Compras', roles: ['DUENO', 'ENCARGADO'] },
  { key: '/clientes', label: 'Clientes', roles: ['DUENO', 'ENCARGADO'] },
  { key: '/reportes', label: 'Reportes', roles: ['DUENO', 'ENCARGADO'] },
  { key: '/comisiones', label: 'Comisiones', roles: ['DUENO'] },
  { key: '/auditoria', label: 'Auditoría', roles: ['DUENO'] },
  { key: '/usuarios', label: 'Usuarios', roles: ['DUENO'] },
];

// Destino de redirect cuando el usuario entra a una ruta que no le corresponde (o hace login).
// "/pos" sigue siendo la pantalla por defecto de siempre para quien puede venderla; para un rol
// que no vende (ej. PRODUCCION) no puede ser un path fijo, así que cae a su primera sección
// disponible en NAV_ITEMS.
export function primeraRutaPermitida(hasRole: (...roles: Rol[]) => boolean): string {
  const pos = NAV_ITEMS.find((item) => item.key === '/pos');
  if (pos && hasRole(...pos.roles)) {
    return '/pos';
  }
  return NAV_ITEMS.find((item) => hasRole(...item.roles))?.key ?? '/login';
}
