CREATE TABLE clientes (
    id                     BIGSERIAL PRIMARY KEY,
    nombre                 VARCHAR(150) NOT NULL,
    telefono               VARCHAR(30),
    tiene_cuenta_corriente BOOLEAN      NOT NULL DEFAULT false,
    activo                 BOOLEAN      NOT NULL DEFAULT true,
    created_at             TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE pagos_cliente (
    id         BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT         NOT NULL REFERENCES clientes (id),
    fecha      TIMESTAMP      NOT NULL DEFAULT now(),
    monto      NUMERIC(12, 2) NOT NULL,
    medio_pago VARCHAR(20)    NOT NULL
);

CREATE INDEX idx_pagos_cliente_cliente ON pagos_cliente (cliente_id);

-- ventas.cliente_id ya existía desde la Fase 1 (nullable, sin FK porque Cliente no existía todavía).
ALTER TABLE ventas
    ADD CONSTRAINT fk_ventas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id);
