# ERP Panadería — Frontend

React + Vite + TypeScript + Ant Design. Cubre de punta a punta lo que hay en el backend: login, caja, venta (POS) con cuenta corriente, productos/insumos/clientes/proveedores, recetas, producción, compras, reportes, comisiones y auditoría — con control de acceso por rol también en el frontend (no solo bloqueado en el backend).

## Requisitos

- Node.js LTS
- El backend corriendo en `http://localhost:8080` (ver [../backend/README.md](../backend/README.md))

## Uso

```bash
npm install
npm run dev
```

Por defecto apunta a `http://localhost:8080` (ver `.env.development`, variable `VITE_API_BASE_URL`).

Usuario de prueba (seed de desarrollo del backend): `admin@panaderia.local` / `changeme123`.

## Qué hay

- **Login** (`/login`): contra `POST /api/auth/login`. Guarda el JWT y los datos del usuario en `localStorage`.
- **`AppLayout`**: header con navegación compartido por todas las pantallas autenticadas, filtrado por rol igual que el backend (`VENDEDOR` solo ve "Venta"; "Comisiones" y "Auditoría" son exclusivos de `DUENO`). La lista de rutas y roles vive en un único lugar (`src/layout/navItems.ts`) que también usa `ProtectedRoute` para bloquear el acceso directo por URL — así el menú y la protección de rutas no pueden desincronizarse.
- **Caja** (`/caja`): abrir turno (monto inicial), cerrarlo (monto final) y registrar ingresos/egresos manuales, contra `POST /api/caja/abrir`, `POST /api/caja/{id}/cerrar` y `POST /api/caja/{id}/movimientos`. Visible para los tres roles (es lo primero que un vendedor necesita usar en su turno).
- **POS** (`/pos`): pantalla de venta.
  - Al entrar, consulta `GET /api/caja/actual`. Si no hay ninguna caja abierta, muestra un aviso con link a "Caja" y deshabilita "Confirmar venta" — sin esto la venta queda con `cajaId` nulo y no aparece agrupada por turno en Comisiones (bug real que motivó agregar la pantalla de Caja).
  - Un input simula el lector láser: se tipea/escanea un código y Enter (o el botón "Agregar") lo resuelve contra `GET /api/ventas/escanear` (distingue código de barras fijo de etiqueta de balanza PLU+peso) y lo agrega al carrito. El foco vuelve automáticamente a ese input después de cada acción (éxito, error o venta confirmada) para no cortar el flujo de un lector físico que escribe donde esté el foco.
  - Cada ítem escaneado muestra una confirmación breve (nombre + cantidad) para feedback inmediato sin tener que mirar el carrito.
  - También se puede buscar un producto manualmente por nombre (Enter en el campo de cantidad también lo agrega) y agregarlo con una cantidad.
  - Si el medio de pago es "Cuenta corriente" aparece un selector de cliente (solo lista los que tienen `tieneCuentaCorriente = true`) y no deja confirmar sin elegir uno.
  - "Confirmar venta" llama a `POST /api/ventas` con el `cajaId` de la caja actual, que descuenta stock en el backend. Atajo de teclado: `Ctrl+Enter` confirma la venta desde cualquier parte de la pantalla sin soltar el mouse.
- **Productos** (`/productos`), **Insumos** (`/insumos`), **Clientes** (`/clientes`): alta/edición en un modal, mismo patrón que Proveedores. Productos permite además dar de baja (soft-delete) y crear una categoría nueva al vuelo desde el propio formulario.
- **Recetas** (`/recetas`): elegís un producto elaborado, cargás/editás los ítems (insumo + cantidad por unidad de producto) y guardás contra `POST`/`PUT /api/produccion/recetas`.
- **Producción** (`/produccion`): elegís un producto con receta, ponés la cantidad a producir, ves una preview de cuánto insumo se va a consumir (calculada en el cliente a partir de la receta) y confirmás contra `POST /api/produccion/ordenes`.
- **Proveedores** (`/proveedores`): alta/edición en un modal, contra `/api/proveedores`.
- **Compras** (`/compras`): elegís proveedor, cargás ítems (insumo, cantidad, costo unitario) y confirmás contra `POST /api/compras`.
- **Reportes** (`/reportes`): un selector de rango de fechas y cuatro pestañas con tablas simples — ventas por período (con desglose por día), productos más vendidos, margen por producto, y stock crítico.
- **Comisiones** (`/comisiones`, solo `DUENO`): mismo selector de rango de fechas, con tablas de comisión de vendedores (por turno) y de producción.
- **Auditoría** (`/auditoria`, solo `DUENO`): tabla con el historial de `GET /api/auditoria`, filtrable por entidad.
- **Usuarios** (`/usuarios`, solo `DUENO`): alta y edición de usuarios (nombre, email, rol, porcentaje de comisión, activo/inactivo y reseteo opcional de contraseña), mismo patrón modal que Clientes/Productos. Contra `POST`/`PUT /api/usuarios`.
- Un interceptor de Axios (`src/api/client.ts`) agrega el JWT a cada request y desloguea automáticamente ante un 401.

## Qué falta (fuera del alcance de esta etapa)

- Pantalla para registrar pagos de cuenta corriente y ver el saldo de un cliente (el backend ya expone `GET /api/clientes/{id}/saldo` y `POST /api/clientes/{id}/pagos`).
- Arqueo de caja (comparar el monto final contado contra el esperado según ventas en efectivo + movimientos) — hoy `Cerrar caja` solo pide el monto contado, sin comparar contra nada. El backend tampoco lo calcula todavía.
- Las pantallas de Usuarios y Caja son agregados fuera de una fase (a pedido explícito, en paralelo a la Fase 4). El resto de "qué falta" en este archivo sigue pendiente de una futura vuelta de pulido del frontend.
- Pulido visual adicional, manejo de sesión expirada más prolijo, tests de frontend.
- Fase 5 del roadmap (generalización a futuro) no se aborda todavía.

## Verificado de punta a punta

Con Docker ya funcionando se pudo probar contra un backend real (no solo simulado): login, escaneo de código de barras en el POS, una venta a cuenta corriente completa (con verificación de que el saldo del cliente se actualiza bien), y los reportes de ventas/comisiones mostrando datos reales de esas ventas. Esto sacó a la luz un bug real que quedó corregido: faltaba configurar CORS en el backend (el browser bloqueaba todas las requests reales del frontend aunque el preflight `OPTIONS` pasara con 200) — nunca se había detectado porque hasta ahora el frontend solo se había probado sin un backend real corriendo en paralelo.

## Estructura

```
src/
├── api/        # cliente Axios + funciones por recurso (auth, usuarios, caja, productos, categorias, inventario, ventas, produccion, compras, clientes, comisiones, reportes, auditoria)
├── auth/       # AuthContext (JWT + usuario en localStorage) y ProtectedRoute (soporta bloqueo por rol)
├── layout/     # AppLayout (header + navegación) y navItems.ts (fuente única de rutas + roles)
├── pages/      # LoginPage, CajaPage, PosPage, ProductosPage, InsumosPage, ClientesPage, RecetasPage, ProduccionPage, ProveedoresPage, ComprasPage, ReportesPage, ComisionesPage, AuditoriaPage, UsuariosPage
└── types/      # tipos TS que reflejan los DTOs del backend
```
