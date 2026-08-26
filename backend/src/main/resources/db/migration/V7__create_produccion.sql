CREATE TABLE recetas (
    id          BIGSERIAL PRIMARY KEY,
    producto_id BIGINT    NOT NULL UNIQUE REFERENCES productos (id),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE receta_items (
    id        BIGSERIAL PRIMARY KEY,
    receta_id BIGINT         NOT NULL REFERENCES recetas (id),
    insumo_id BIGINT         NOT NULL REFERENCES insumos (id),
    cantidad  NUMERIC(12, 3) NOT NULL
);

CREATE INDEX idx_receta_items_receta ON receta_items (receta_id);

CREATE TABLE ordenes_produccion (
    id          BIGSERIAL PRIMARY KEY,
    producto_id BIGINT         NOT NULL REFERENCES productos (id),
    cantidad    NUMERIC(12, 3) NOT NULL,
    fecha       TIMESTAMP      NOT NULL DEFAULT now(),
    estado      VARCHAR(20)    NOT NULL,
    usuario_id  BIGINT         NOT NULL REFERENCES usuarios (id)
);

CREATE INDEX idx_ordenes_produccion_producto ON ordenes_produccion (producto_id);
