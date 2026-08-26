export type Rol = 'DUENO' | 'ENCARGADO' | 'VENDEDOR';

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  rol: Rol;
  activo: boolean;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInMinutes: number;
  usuario: Usuario;
}

export type TipoProducto = 'ELABORADO' | 'REVENTA';
export type UnidadMedida = 'UNIDAD' | 'KG' | 'GRAMO' | 'LITRO';

export interface Producto {
  id: number;
  nombre: string;
  categoriaId: number;
  categoriaNombre: string;
  tipo: TipoProducto;
  seVendePorPeso: boolean;
  precioVenta: number;
  unidadMedida: UnidadMedida;
  codigoBarras: string | null;
  codigoPLU: string | null;
  stockActual: number;
  stockMinimo: number;
  activo: boolean;
}

export type MedioPago =
  | 'EFECTIVO'
  | 'TARJETA_DEBITO'
  | 'TARJETA_CREDITO'
  | 'TRANSFERENCIA'
  | 'CUENTA_CORRIENTE';

export const MEDIOS_PAGO: { value: MedioPago; label: string }[] = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'TARJETA_DEBITO', label: 'Tarjeta de débito' },
  { value: 'TARJETA_CREDITO', label: 'Tarjeta de crédito' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
  { value: 'CUENTA_CORRIENTE', label: 'Cuenta corriente' },
];

export interface EscaneoResponse {
  productoId: number;
  productoNombre: string;
  seVendePorPeso: boolean;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface DetalleVentaResponse {
  id: number;
  productoId: number;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface VentaResponse {
  id: number;
  fecha: string;
  clienteId: number | null;
  usuarioId: number;
  cajaId: number | null;
  total: number;
  medioPago: MedioPago;
  estado: string;
  detalles: DetalleVentaResponse[];
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  campos?: Record<string, string>;
}

export interface Insumo {
  id: number;
  nombre: string;
  unidadMedida: UnidadMedida;
  stockActual: number;
  stockMinimo: number;
  costoUnitario: number;
}

export interface RecetaItem {
  insumoId: number;
  insumoNombre: string;
  unidadMedida: string;
  cantidad: number;
}

export interface Receta {
  id: number;
  productoId: number;
  productoNombre: string;
  items: RecetaItem[];
}

export interface OrdenProduccion {
  id: number;
  productoId: number;
  productoNombre: string;
  cantidad: number;
  fecha: string;
  estado: string;
  usuarioId: number;
}

export interface Proveedor {
  id: number;
  nombre: string;
  contacto: string | null;
  telefono: string | null;
  email: string | null;
  activo: boolean;
}

export interface DetalleCompra {
  id: number;
  insumoId: number;
  insumoNombre: string;
  cantidad: number;
  costoUnitario: number;
  subtotal: number;
}

export interface Compra {
  id: number;
  proveedorId: number;
  proveedorNombre: string;
  fecha: string;
  total: number;
  estado: string;
  detalles: DetalleCompra[];
}
