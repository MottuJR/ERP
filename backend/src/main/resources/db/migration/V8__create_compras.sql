CREATE TABLE proveedores (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(150) NOT NULL,
    contacto   VARCHAR(150),
    telefono   VARCHAR(30),
    email      VARCHAR(160),
    activo     BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE compras (
    id           BIGSERIAL PRIMARY KEY,
    proveedor_id BIGINT         NOT NULL REFERENCES proveedores (id),
    fecha        TIMESTAMP      NOT NULL DEFAULT now(),
    total        NUMERIC(12, 2) NOT NULL,
    estado       VARCHAR(20)    NOT NULL
);

CREATE INDEX idx_compras_proveedor ON compras (proveedor_id);

CREATE TABLE detalle_compras (
    id             BIGSERIAL PRIMARY KEY,
    compra_id      BIGINT         NOT NULL REFERENCES compras (id),
    insumo_id      BIGINT         NOT NULL REFERENCES insumos (id),
    cantidad       NUMERIC(12, 3) NOT NULL,
    costo_unitario NUMERIC(12, 2) NOT NULL,
    subtotal       NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_detalle_compras_compra ON detalle_compras (compra_id);
