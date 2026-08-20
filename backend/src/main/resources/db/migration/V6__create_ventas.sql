CREATE TABLE ventas (
    id          BIGSERIAL PRIMARY KEY,
    fecha       TIMESTAMP      NOT NULL DEFAULT now(),
    cliente_id  BIGINT,
    usuario_id  BIGINT         NOT NULL REFERENCES usuarios (id),
    caja_id     BIGINT         REFERENCES cajas (id),
    total       NUMERIC(12, 2) NOT NULL,
    medio_pago  VARCHAR(20)    NOT NULL,
    estado      VARCHAR(20)    NOT NULL
);

CREATE INDEX idx_ventas_fecha ON ventas (fecha);
CREATE INDEX idx_ventas_caja ON ventas (caja_id);
CREATE INDEX idx_ventas_usuario ON ventas (usuario_id);

CREATE TABLE detalle_ventas (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT         NOT NULL REFERENCES ventas (id),
    producto_id     BIGINT         NOT NULL REFERENCES productos (id),
    cantidad        NUMERIC(12, 3) NOT NULL,
    precio_unitario NUMERIC(12, 2) NOT NULL,
    subtotal        NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_detalle_ventas_venta ON detalle_ventas (venta_id);
CREATE INDEX idx_detalle_ventas_producto ON detalle_ventas (producto_id);
