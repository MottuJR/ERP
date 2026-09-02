# ERP Panadería — Backend

Fase 0 (setup: proyecto base, Postgres vía Flyway, autenticación JWT), Fase 1 (Productos, Inventario, Caja, Ventas/POS) y Fase 2 (Producción, Compras) del roadmap.

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

## Endpoints de productos e inventario

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/categorias` | autenticado | Lista categorías |
| POST/PUT | `/api/categorias` | `DUENO`/`ENCARGADO` | Alta/edición de categoría |
| GET | `/api/productos` | autenticado | Lista productos activos |
| GET | `/api/productos/{id}` | autenticado | Detalle de un producto |
| GET | `/api/productos/codigo/{codigo}` | autenticado | Busca por código de barras fijo |
| POST/PUT/DELETE | `/api/productos/**` | `DUENO`/`ENCARGADO` | Alta/edición/baja (soft-delete) |
| GET | `/api/inventario/insumos` | autenticado | Lista insumos |
| POST | `/api/inventario/insumos` | `DUENO`/`ENCARGADO` | Alta de insumo |
| GET | `/api/inventario/movimientos?itemTipo=&itemId=` | autenticado | Historial de movimientos de stock |
| POST | `/api/inventario/movimientos` | `DUENO`/`ENCARGADO` | Movimiento manual (entrada/salida/ajuste/merma) |

## Endpoints de caja

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/caja/actual` | Caja abierta actualmente (404 si no hay ninguna) |
| POST | `/api/caja/abrir` | Abre un turno (`{ "montoInicial": ... }`) — falla si ya hay una caja abierta |
| POST | `/api/caja/{id}/cerrar` | Cierra el turno (`{ "montoFinal": ... }`) |
| GET/POST | `/api/caja/{id}/movimientos` | Ingresos/egresos manuales de esa caja |

## Endpoints de ventas (POS)

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/ventas/escanear?codigo=...` | Resuelve lo que devolvió el lector láser (código fijo o etiqueta de balanza PLU+peso) y devuelve una preview con cantidad/precio/subtotal, sin confirmar nada |
| POST | `/api/ventas` | Confirma la venta: recibe el carrito (ítems por código escaneado o por `productoId`+`cantidad`), descuenta stock automáticamente y registra el `MovimientoStock` correspondiente |
| GET | `/api/ventas/{id}` | Detalle de una venta |

**Importante sobre el parseo de código de balanza:** `EscaneoService` asume un esquema de 13 dígitos (prefijo 20-29 + 5 dígitos de PLU + 5 dígitos de peso en gramos + dígito verificador), que es el más común en Argentina pero varía por fabricante. Hay que confirmarlo contra el manual de la balanza real antes de ir a producción — las constantes de parseo están todas juntas al principio de la clase.

## Endpoints de producción

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/produccion/recetas/{productoId}` | Receta de un producto elaborado |
| POST | `/api/produccion/recetas` | Carga la receta de un producto (`{ productoId, items: [{ insumoId, cantidad }] }`) — solo productos `ELABORADO`, uno por producto |
| PUT | `/api/produccion/recetas/{productoId}` | Reemplaza los ítems de la receta |
| POST | `/api/produccion/ordenes` | Confirma una orden de producción (`{ productoId, cantidad }`): descuenta cada insumo de la receta (cantidad de la receta × cantidad a producir) y suma el stock del producto terminado, todo en una transacción |
| GET | `/api/produccion/ordenes/{id}` | Detalle de una orden |

Acceso: `DUENO`/`ENCARGADO` (no `VENDEDOR`).

**Mermas:** no hay un endpoint separado — se registran con el mismo `POST /api/inventario/movimientos` (`tipo: MERMA`) de la Fase 1, sobre el insumo o producto que corresponda. No tenía sentido duplicar esa lógica acá.

## Endpoints de compras

| Método | Endpoint | Descripción |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/proveedores/**` | CRUD de proveedores (`DELETE` es baja lógica) |
| POST | `/api/compras` | Confirma una compra (`{ proveedorId, items: [{ insumoId, cantidad, costoUnitario }] }`): suma stock de cada insumo recibido y actualiza su `costoUnitario` al último precio pagado |
| GET | `/api/compras/{id}` | Detalle de una compra |

Acceso: `DUENO`/`ENCARGADO`.

**Política de costo:** `costoUnitario` del insumo se pisa con el precio de la última compra (sin promedio ponderado). Es la opción más simple para el MVP; si hace falta costo promedio ponderado más adelante, es un cambio acotado a `InventarioService.registrarEntradaInsumoPorCompra`.

## Roles

`DUENO`, `ENCARGADO`, `VENDEDOR` (ver `com.panaderia.erp.core.usuario.Rol`). Gestión de usuarios, productos, categorías, insumos y ajustes manuales de stock: `DUENO`/`ENCARGADO`. Caja y ventas: los tres roles (`VENDEDOR` incluido, ya que es el que opera el POS).

## Configuración por entorno

- `application.yml`: configuración común (perfil activo por defecto: `dev`).
- `application-dev.yml`: datasource y `JWT_SECRET` con defaults para desarrollo local (no usar en producción).
- `application-prod.yml`: todo se toma de variables de entorno, sin defaults — falla rápido si falta algo.

Variables de entorno relevantes: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET`, `DB_HOST`, `DB_PORT`.

## Tests

```bash
./mvnw test
```

Esto corre los tests unitarios (JUnit/Mockito) más `ErpApplicationTests`. **Los tests de integración (que terminan en `*IT`, por ejemplo `AuthControllerIT` y `ProduccionCompraFlowIT`) no se ejecutan con este comando** — Surefire por defecto solo agarra `*Test`/`*Tests` (el patrón `*IT` es de Failsafe, que este proyecto no tiene configurado). Para correrlos:

```bash
./mvnw test "-Dtest=AuthControllerIT,ProduccionCompraFlowIT"
```

Los `*IT` usan Testcontainers y levantan un Postgres real en Docker — Docker Desktop tiene que estar corriendo. El contenedor se comparte entre todas las clases de test dentro de la misma corrida (patrón "singleton container": se levanta una vez en un bloque estático en `AbstractIntegrationTest`, no vía `@Testcontainers`/`@Container`, porque esas anotaciones apagan el contenedor al terminar la primera clase que lo usa, rompiendo las que corren después).
