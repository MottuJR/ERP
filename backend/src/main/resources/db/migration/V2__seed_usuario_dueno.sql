-- Usuario inicial para poder loguearse la primera vez.
-- Credenciales de desarrollo, documentadas en el README del backend.
-- IMPORTANTE: cambiar la contraseña (o borrar este usuario) antes de ir a producción.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO usuarios (nombre, email, password_hash, rol, activo)
VALUES (
    'Administrador',
    'admin@panaderia.local',
    crypt('changeme123', gen_salt('bf', 10)),
    'DUENO',
    true
);
