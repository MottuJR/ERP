# ERP Panadería — Backend

Fase 0 del roadmap: proyecto Spring Boot base, conexión a Postgres vía Flyway, y módulo de autenticación (usuarios, roles, login con JWT).

## Stack

- Java 21 + Spring Boot 4.1
- Spring Data JPA + Hibernate
- PostgreSQL + Flyway
- Spring Security + JWT (jjwt)

## Requisitos

- JDK 21
- Docker + Docker Compose (para levantar Postgres en desarrollo, y para correr los tests de integración con Testcontainers)

## Levantar el entorno de desarrollo

1. Levantar Postgres (desde la raíz del repo, donde está `docker-compose.yml`):

   ```bash
   docker compose up -d
   ```

2. Correr el backend con el perfil `dev` (ya es el perfil por defecto):

   ```bash
   ./mvnw spring-boot:run
   ```

   La app queda escuchando en `http://localhost:8080`. Flyway corre las migraciones automáticamente al arrancar.

## Usuario inicial (seed de desarrollo)

La migración `V2__seed_usuario_dueno.sql` crea un usuario dueño para poder loguearse la primera vez:

- **email:** `admin@panaderia.local`
- **password:** `changeme123`

Esto es solo para desarrollo. Antes de ir a producción hay que cambiar la contraseña o borrar este usuario.

## Endpoints de auth

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | público | `{ "email": "...", "password": "..." }` → devuelve JWT |
| GET | `/api/auth/me` | autenticado | Datos del usuario logueado |
| GET | `/api/usuarios` | rol `DUENO` | Lista todos los usuarios |
| POST | `/api/usuarios` | rol `DUENO` | Crea un usuario nuevo (encargado/vendedor/etc.) |

Para los endpoints protegidos, mandar el JWT en el header:

```
Authorization: Bearer <token>
```

## Roles

`DUENO`, `ENCARGADO`, `VENDEDOR` (ver `com.panaderia.erp.core.usuario.Rol`). Por ahora solo `DUENO` tiene una restricción explícita (gestión de usuarios); el resto de la autorización fina se define a medida que se agreguen los módulos de negocio.

## Configuración por entorno

- `application.yml`: configuración común (perfil activo por defecto: `dev`).
- `application-dev.yml`: datasource y `JWT_SECRET` con defaults para desarrollo local (no usar en producción).
- `application-prod.yml`: todo se toma de variables de entorno, sin defaults — falla rápido si falta algo.

Variables de entorno relevantes: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET`, `DB_HOST`, `DB_PORT`.

## Tests

```bash
./mvnw test
```

Los tests de integración (`*IT`, por ejemplo `AuthControllerIT`) usan Testcontainers y levantan un Postgres real en Docker — Docker tiene que estar corriendo. `JwtServiceTest` es un test unitario puro, no necesita Docker.
