// Protocolo de comunicación entre el POS (pestaña del mostrador) y la pantalla del cliente
// (segunda ventana, pensada para un segundo monitor). Todo vía BroadcastChannel del navegador,
// sin pasar por el backend: ambas pestañas viven en el mismo navegador de la misma PC.

export const CANAL_PANTALLA_CLIENTE = 'erp-pos-pantalla-cliente';

export interface ItemPantallaCliente {
  productoNombre: string;
  cantidad: number;
  seVendePorPeso: boolean;
  unidadMedida: string;
  precioUnitario: number;
  subtotal: number;
}

export type MensajePantallaCliente =
  // El POS manda esto cada vez que cambia el carrito (agregar/quitar ítem, cambiar cantidad).
  | { tipo: 'actualizar'; items: ItemPantallaCliente[]; total: number }
  // El POS manda esto una vez al confirmar la venta, antes de vaciar el carrito.
  | { tipo: 'venta-confirmada'; total: number }
  // La pantalla del cliente manda esto al abrirse, por si el POS ya tiene un carrito en curso
  // (por ejemplo, si se la abre o se recarga a mitad de una venta).
  | { tipo: 'solicitar-estado' };
