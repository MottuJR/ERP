package com.panaderia.erp.caja;

/**
 * Cómo se le paga a la vendedora su comisión del turno al momento de cerrar la caja. Si es
 * EFECTIVO, ese monto se descuenta del efectivo esperado porque sale físicamente del cajón.
 */
public enum MedioPagoComision {
    EFECTIVO,
    TRANSFERENCIA
}
