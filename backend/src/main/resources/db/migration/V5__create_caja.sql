CREATE TABLE cajas (
    id              BIGSERIAL PRIMARY KEY,
    fecha_apertura  TIMESTAMP      NOT NULL DEFAULT now(),
    fecha_cierre    TIMESTAMP,
    monto_inicial   NUMERIC(12, 2) NOT NULL,
    monto_final     NUMERIC(12, 2),
    usuario_id      BIGINT         NOT NULL REFERENCES usuarios (id),
    estado          VARCHAR(20)    NOT NULL
);

CREATE INDEX idx_cajas_estado ON cajas (estado);

CREATE TABLE movimientos_caja (
    id       BIGSERIAL PRIMARY KEY,
    caja_id  BIGINT         NOT NULL REFERENCES cajas (id),
    tipo     VARCHAR(20)    NOT NULL,
    monto    NUMERIC(12, 2) NOT NULL,
    concepto VARCHAR(255)   NOT NULL,
    fecha    TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_movimientos_caja_caja ON movimientos_caja (caja_id);
