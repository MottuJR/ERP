import type { ComponentType } from 'react';
import {
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  BookOutlined,
  DollarOutlined,
  HistoryOutlined,
  InboxOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
  TeamOutlined,
  ToolOutlined,
  TruckOutlined,
  UserOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import type { Rol } from '../types';

export interface NavItem {
  key: string;
  label: string;
  roles: Rol[];
  // Componente de ícono (no JSX): este archivo es .ts, no .tsx — AppLayout es quien lo instancia.
  icon: ComponentType;
}

// Única fuente de verdad para qué roles pueden ver cada sección: la usan tanto el menú
// (para no mostrar links a los que no tienen acceso) como las rutas (para bloquear el acceso
// directo por URL) — así no hay forma de que queden desincronizados. El ícono es solo estético
// (ayuda a separar visualmente un módulo de otro en el menú), no tiene ninguna otra lógica atada.
export const NAV_ITEMS: NavItem[] = [
  { key: '/caja', label: 'Caja', roles: ['DUENO', 'ENCARGADO', 'VENDEDOR'], icon: WalletOutlined },
  { key: '/historial-cajas', label: 'Historial de cajas', roles: ['DUENO', 'ENCARGADO'], icon: HistoryOutlined },
  { key: '/pos', label: 'Venta', roles: ['DUENO', 'ENCARGADO', 'VENDEDOR'], icon: ShoppingCartOutlined },
  { key: '/productos', label: 'Productos', roles: ['DUENO', 'ENCARGADO'], icon: AppstoreOutlined },
  { key: '/insumos', label: 'Insumos', roles: ['DUENO', 'ENCARGADO', 'PRODUCCION'], icon: InboxOutlined },
  { key: '/recetas', label: 'Recetas', roles: ['DUENO', 'ENCARGADO', 'PRODUCCION'], icon: BookOutlined },
  { key: '/produccion', label: 'Producción', roles: ['DUENO', 'ENCARGADO', 'PRODUCCION'], icon: ToolOutlined },
  { key: '/proveedores', label: 'Proveedores', roles: ['DUENO', 'ENCARGADO'], icon: TruckOutlined },
  { key: '/compras', label: 'Compras', roles: ['DUENO', 'ENCARGADO'], icon: ShoppingOutlined },
  { key: '/clientes', label: 'Clientes', roles: ['DUENO', 'ENCARGADO'], icon: TeamOutlined },
  { key: '/reportes', label: 'Reportes', roles: ['DUENO', 'ENCARGADO'], icon: BarChartOutlined },
  { key: '/comisiones', label: 'Comisiones', roles: ['DUENO'], icon: DollarOutlined },
  { key: '/auditoria', label: 'Auditoría', roles: ['DUENO'], icon: AuditOutlined },
  { key: '/usuarios', label: 'Usuarios', roles: ['DUENO'], icon: UserOutlined },
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
