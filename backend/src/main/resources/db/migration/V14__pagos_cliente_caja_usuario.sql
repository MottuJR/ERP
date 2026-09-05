-- Para que un cobro de cuenta corriente entre en la contabilidad de efectivo del turno y en la
-- comisión de quien lo cobró, igual que una venta. Nullable porque los pagos ya cargados no
-- tienen esta información, y porque un cobro puede hacerse sin ninguna caja abierta.
ALTER TABLE pagos_cliente ADD COLUMN caja_id BIGINT REFERENCES cajas (id);
ALTER TABLE pagos_cliente ADD COLUMN usuario_id BIGINT REFERENCES usuarios (id);

CREATE INDEX idx_pagos_cliente_caja ON pagos_cliente (caja_id);
