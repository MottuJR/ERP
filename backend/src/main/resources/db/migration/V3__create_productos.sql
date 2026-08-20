CREATE TABLE categorias (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(80) NOT NULL UNIQUE,
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE productos (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(150)   NOT NULL,
    categoria_id    BIGINT         NOT NULL REFERENCES categorias (id),
    tipo            VARCHAR(20)    NOT NULL,
    se_vende_por_peso BOOLEAN      NOT NULL DEFAULT false,
    precio_venta    NUMERIC(12, 2) NOT NULL,
    unidad_medida   VARCHAR(20)    NOT NULL,
    codigo_barras   VARCHAR(32)    UNIQUE,
    codigo_plu      VARCHAR(16)    UNIQUE,
    stock_actual    NUMERIC(12, 3) NOT NULL DEFAULT 0,
    stock_minimo    NUMERIC(12, 3) NOT NULL DEFAULT 0,
    activo          BOOLEAN        NOT NULL DEFAULT true,
    created_at      TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT chk_producto_peso_variable CHECK (
        (se_vende_por_peso = true AND codigo_plu IS NOT NULL AND codigo_barras IS NULL)
        OR
        (se_vende_por_peso = false AND codigo_plu IS NULL)
    )
);

CREATE INDEX idx_productos_categoria ON productos (categoria_id);
CREATE INDEX idx_productos_activo ON productos (activo);
