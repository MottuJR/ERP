CREATE TABLE insumos (
    id             BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(150)   NOT NULL,
    unidad_medida  VARCHAR(20)    NOT NULL,
    stock_actual   NUMERIC(12, 3) NOT NULL DEFAULT 0,
    stock_minimo   NUMERIC(12, 3) NOT NULL DEFAULT 0,
    costo_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at     TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE TABLE movimientos_stock (
    id            BIGSERIAL PRIMARY KEY,
    tipo          VARCHAR(20)    NOT NULL,
    item_tipo     VARCHAR(20)    NOT NULL,
    item_id       BIGINT         NOT NULL,
    cantidad      NUMERIC(12, 3) NOT NULL,
    fecha         TIMESTAMP      NOT NULL DEFAULT now(),
    motivo        VARCHAR(255),
    referencia_id BIGINT
);

CREATE INDEX idx_movimientos_stock_item ON movimientos_stock (item_tipo, item_id);
