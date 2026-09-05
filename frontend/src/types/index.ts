export type Rol = 'DUENO' | 'ENCARGADO' | 'VENDEDOR' | 'PRODUCCION';

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  rol: Rol;
  activo: boolean;
  porcentajeComision: number | null;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInMinutes: number;
  usuario: Usuario;
}

export type TipoProducto = 'ELABORADO' | 'REVENTA';
export type UnidadMedida = 'UNIDAD' | 'KG' | 'GRAMO' | 'LITRO';

// La unidad real de un producto/insumo es unidadMedida, no seVendePorPeso (que solo dice si usa
// código de barras fijo o etiqueta de balanza PLU) — con esto evitamos mostrar "kg" para un
// producto cuya unidad de medida es en realidad "unidad".
export const ABREVIATURA_UNIDAD_MEDIDA: Record<UnidadMedida, string> = {
  UNIDAD: 'unidades',
  KG: 'kg',
  GRAMO: 'g',
  LITRO: 'L',
};

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

export type EstadoCaja = 'ABIERTA' | 'CERRADA';
export type TipoMovimientoCaja = 'INGRESO' | 'EGRESO';

export interface Caja {
  id: number;
  fechaApertura: string;
  fechaCierre: string | null;
  montoInicial: number;
  montoFinal: number | null;
  usuarioId: number;
  estado: EstadoCaja;
}

export interface MovimientoCaja {
  id: number;
  cajaId: number;
  tipo: TipoMovimientoCaja;
  monto: number;
  concepto: string;
  fecha: string;
}

export interface CajaHistorial {
  id: number;
  fechaApertura: string;
  fechaCierre: string | null;
  montoInicial: number;
  montoFinal: number | null;
  usuarioId: number;
  usuarioNombre: string;
  estado: EstadoCaja;
}

export interface VentaPorMedioPago {
  medioPago: MedioPago;
  total: number;
  cantidad: number;
}

export interface VentaResumenCaja {
  id: number;
  fecha: string;
  medioPago: MedioPago;
  total: number;
  usuarioNombre: string;
  clienteNombre: string | null;
}

export interface CajaResumen {
  id: number;
  fechaApertura: string;
  fechaCierre: string | null;
  montoInicial: number;
  montoFinal: number | null;
  usuarioId: number;
  usuarioNombre: string;
  estado: EstadoCaja;
  ventasPorMedioPago: VentaPorMedioPago[];
  totalVentas: number;
  totalIngresos: number;
  totalEgresos: number;
  efectivoEsperado: number;
  diferencia: number | null;
  ventas: VentaResumenCaja[];
}

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
  rendimiento: number;
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

export interface Cliente {
  id: number;
  nombre: string;
  telefono: string | null;
  tieneCuentaCorriente: boolean;
  activo: boolean;
}

export interface PagoCliente {
  id: number;
  clienteId: number;
  fecha: string;
  monto: number;
  medioPago: MedioPago;
}

export interface SaldoCliente {
  clienteId: number;
  clienteNombre: string;
  totalVentasCuentaCorriente: number;
  totalPagos: number;
  saldo: number;
}

export interface ComisionVendedor {
  cajaId: number;
  usuarioId: number;
  usuarioNombre: string;
  totalVendido: number;
  porcentaje: number | null;
  comision: number;
}

export interface ComisionProduccion {
  ordenId: number;
  usuarioId: number;
  usuarioNombre: string;
  productoId: number;
  productoNombre: string;
  cantidadProducida: number;
  precioProducto: number;
  porcentaje: number | null;
  comision: number;
}

export interface VentaDia {
  fecha: string;
  cantidadVentas: number;
  totalVendido: number;
}

export interface ReporteVentas {
  desde: string;
  hasta: string;
  cantidadVentas: number;
  totalVendido: number;
  promedioPorVenta: number;
  porDia: VentaDia[];
}

export interface ProductoMasVendido {
  productoId: number;
  productoNombre: string;
  cantidadVendida: number;
  montoTotal: number;
}

export interface MargenProducto {
  productoId: number;
  productoNombre: string;
  precioVenta: number;
  costoInsumos: number;
  margen: number;
  margenPorcentual: number;
}

export interface StockCriticoItem {
  id: number;
  nombre: string;
  stockActual: number;
  stockMinimo: number;
}

export interface StockCritico {
  productos: StockCriticoItem[];
  insumos: StockCriticoItem[];
}
