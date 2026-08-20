package com.panaderia.erp.ventas;

public enum MedioPago {
    EFECTIVO,
    TARJETA_DEBITO,
    TARJETA_CREDITO,
    TRANSFERENCIA,
    // Habilitado desde ya (aunque el manejo de cuentas corrientes de clientes es una fase futura)
    // para no perder, en el detalle de cada venta, qué se llevó el cliente a cuenta.
    CUENTA_CORRIENTE
}
