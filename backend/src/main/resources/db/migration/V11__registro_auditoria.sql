CREATE TABLE registro_auditoria (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios (id),
    entidad    VARCHAR(60)  NOT NULL,
    entidad_id BIGINT,
    accion     VARCHAR(40)  NOT NULL,
    fecha      TIMESTAMP    NOT NULL DEFAULT now(),
    detalle    VARCHAR(500)
);

CREATE INDEX idx_registro_auditoria_entidad ON registro_auditoria (entidad, entidad_id);
CREATE INDEX idx_registro_auditoria_fecha ON registro_auditoria (fecha);
