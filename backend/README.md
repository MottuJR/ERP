# ERP Panadería — Backend

Fase 0 (setup: proyecto base, Postgres vía Flyway, autenticación JWT), Fase 1 (Productos, Inventario, Caja, Ventas/POS), Fase 2 (Producción, Compras), Fase 3 (Reportes, Comisiones/Liquidaciones, Cuentas corrientes) y Fase 4 (Pulido: permisos finos por rol, auditoría de cambios) del roadmap.

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
| PUT | `/api/inventario/insumos/{id}` | `DUENO`/`ENCARGADO` | Edición de datos maestros del insumo (nombre, unidad, stock mínimo, costo) — no toca `stockActual` |
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

## Endpoints de reportes

Todos con `desde`/`hasta` como fecha `yyyy-MM-dd` (rango inclusivo).

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/reportes/ventas?desde=&hasta=` | Total vendido, cantidad de ventas, promedio y desglose por día |
| GET | `/api/reportes/productos-mas-vendidos?desde=&hasta=&limite=10` | Ranking por cantidad vendida |
| GET | `/api/reportes/margen-productos` | Para cada producto `ELABORADO` con receta: `precioVenta - costo de insumos` |
| GET | `/api/reportes/stock-critico` | Productos e insumos con `stockActual <= stockMinimo` |

Acceso: `DUENO`/`ENCARGADO`. `margen-productos` solo incluye productos con receta cargada (sin receta no hay forma de calcular el costo).

## Endpoints de comisiones/liquidaciones

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/comisiones/vendedores?desde=&hasta=` | Por cada (turno, vendedor): `total vendido en ese turno × porcentaje de comisión del vendedor` |
| GET | `/api/comisiones/produccion?desde=&hasta=` | Por cada orden de producción: `cantidad producida × precio del producto × porcentaje de comisión del empleado` |

Acceso: solo `DUENO` (es información de sueldos). El porcentaje de comisión vive en `Usuario.porcentajeComision` (nullable — un usuario sin porcentaje asignado da comisión 0), editable con `PUT /api/usuarios/{id}/comision`. **Se calcula on-demand, no se persiste como movimiento** — es una de las decisiones que la sección 7 del documento dejaba pendientes, resuelta así para esta fase.

## Endpoints de clientes y cuenta corriente

| Método | Endpoint | Descripción |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/clientes/**` | CRUD de clientes (`tieneCuentaCorriente` habilita que se les venda a cuenta) |
| GET | `/api/clientes/{id}/saldo` | `suma(Venta.total con medioPago=CUENTA_CORRIENTE) - suma(PagoCliente.monto)` |
| GET/POST | `/api/clientes/{id}/pagos` | Historial y registro de pagos contra la cuenta |

Al confirmar una venta con `medioPago: CUENTA_CORRIENTE`, `VentaService` exige `clienteId` y valida que ese cliente tenga `tieneCuentaCorriente = true` (si no, `400`). `POST /api/clientes/{id}/pagos` requiere `DUENO`/`ENCARGADO` (es manejo de caja/cobranza, no algo que un `VENDEDOR` deba poder hacer sin supervisión).

## Roles

`DUENO`, `ENCARGADO`, `VENDEDOR` (ver `com.panaderia.erp.core.usuario.Rol`). Gestión de usuarios, productos, categorías, insumos, clientes y ajustes manuales de stock: `DUENO`/`ENCARGADO`. Caja y ventas: los tres roles (`VENDEDOR` incluido, ya que es el que opera el POS). Auditoría: solo `DUENO`. La lista completa de accesos por endpoint está reflejada en las tablas de arriba; el criterio general es "consulta abierta a los tres roles autenticados, escritura restringida según sensibilidad".

## Auditoría de cambios

`GET /api/auditoria?entidad=&entidadId=` (solo `DUENO`) devuelve el historial de la tabla genérica `registro_auditoria` (`usuarioId`, `entidad`, `entidadId`, `accion`, `fecha`, `detalle`), poblada vía `AuditoriaService.registrar(...)` desde dentro de las mismas transacciones de negocio — no es un mecanismo separado (AOP/interceptor), sigue el mismo patrón que ya se usaba para pasar el usuario logueado a los servicios (`Authentication` → email → servicio).

Operaciones auditadas: alta/edición/baja de productos (con detalle especial cuando cambia el precio), ajustes manuales de stock (`POST /api/inventario/movimientos`), apertura y cierre de caja. **No** se audita: los movimientos de stock automáticos de ventas/producción/compras (ya tienen su propia trazabilidad vía `MovimientoStock.referenciaId`), ni los ingresos/egresos de caja dentro de un turno (solo apertura/cierre).

## Configuración por entorno

- `application.yml`: configuración común (perfil activo por defecto: `dev`).
- `application-dev.yml`: datasource y `JWT_SECRET` con defaults para desarrollo local (no usar en producción).
- `application-prod.yml`: todo se toma de variables de entorno, sin defaults — falla rápido si falta algo.

Variables de entorno relevantes: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET`, `DB_HOST`, `DB_PORT`, `CORS_ALLOWED_ORIGINS`.

**CORS:** el frontend (`http://localhost:5173` en dev) y el backend (`:8080`) son orígenes distintos, así que hace falta CORS habilitado explícitamente (`SecurityConfig.corsConfigurationSource`) — sin esto el browser bloquea las requests reales aunque el preflight `OPTIONS` responda 200. En dev ya viene con ese default; en producción hay que setear `CORS_ALLOWED_ORIGINS` con el dominio real del frontend (sin default, a propósito).

## Tests

```bash
./mvnw test
```

Esto corre los tests unitarios (JUnit/Mockito) más `ErpApplicationTests`. **Los tests de integración (que terminan en `*IT`, por ejemplo `AuthControllerIT` y `ProduccionCompraFlowIT`) no se ejecutan con este comando** — Surefire por defecto solo agarra `*Test`/`*Tests` (el patrón `*IT` es de Failsafe, que este proyecto no tiene configurado). Para correrlos:

```bash
./mvnw test "-Dtest=AuthControllerIT,ProduccionCompraFlowIT"
```

Los `*IT` usan Testcontainers y levantan un Postgres real en Docker — Docker Desktop tiene que estar corriendo. El contenedor se comparte entre todas las clases de test dentro de la misma corrida (patrón "singleton container": se levanta una vez en un bloque estático en `AbstractIntegrationTest`, no vía `@Testcontainers`/`@Container`, porque esas anotaciones apagan el contenedor al terminar la primera clase que lo usa, rompiendo las que corren después).
