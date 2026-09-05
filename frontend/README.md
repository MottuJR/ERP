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
  - Clientes muestra además una columna "Saldo" para los que tienen cuenta corriente habilitada (`GET /api/clientes/{id}/saldo` por cada uno) y un botón "Cuenta corriente" que abre una sección "Compras" (con filtro por rango de fechas, `GET /api/clientes/{id}/ventas`), el historial de pagos y un formulario para registrar uno nuevo (`GET`/`POST /api/clientes/{id}/pagos`) — el saldo siempre fue calculado bien en el backend (ventas a cuenta corriente menos pagos), pero antes no había ninguna pantalla que lo mostrara.
    - Un cobro de cuenta corriente queda atado a la caja abierta en ese momento (si hay una) y a quién lo cobró, igual que una venta: si se avisa que no había ninguna caja abierta, es porque ese cobro no va a entrar en la contabilidad de efectivo de ningún turno ni en la comisión de nadie.
- **Recetas** (`/recetas`): elegís un producto elaborado, cargás/editás los ítems (insumo + cantidad **de toda la tanda**, no por unidad de producto) más el **rendimiento** (cuánto da esa tanda, en la unidad de venta del producto — kg o unidades) y guardás contra `POST`/`PUT /api/produccion/recetas`. Esto existe porque una tanda de insumos no rinde 1:1 en producto terminado (1 kg de masa de medialunas puede rendir 40 unidades; el harina + otros insumos de un pan puede rendir 1,4 kg de pan). Las recetas cargadas antes de este cambio quedaron con rendimiento = 1 (sin cambios, porque ya estaban escritas por unidad de producto).
  - Por cada ítem muestra su costo (`costoUnitario` del insumo × cantidad) y el costo total de la tanda — todo calculado en el cliente a partir de los insumos ya cargados, sin pedirle nada nuevo al backend. El costo por unidad vendible (el que importa para el margen) es ese costo total dividido por el rendimiento.
  - Con el precio de venta actual del producto calcula el margen actual **como markup sobre el costo por unidad** (`(precioVenta - costoPorUnidad) / costoPorUnidad`, sin techo) — a propósito distinto del "margen %" que muestra `GET /api/reportes/margen-productos` (que es `(precioVenta - costo) / precioVenta`, ese sí acotado a <100% por definición). Un campo de "margen deseado" (sin límite superior, puede ser 2000% o lo que haga falta) sugiere el precio de venta correspondiente, y "Aplicar como precio de venta" lo guarda contra `PUT /api/productos/{id}`.
- **Producción** (`/produccion`): elegís un producto con receta, ponés cuántas veces se hace esa receta (no cuántas unidades de producto querés — eso lo calcula el rendimiento), ves una preview de cuánto insumo se va a consumir y cuánto stock de producto terminado se va a sumar (calculado en el cliente a partir de la receta) y confirmás contra `POST /api/produccion/ordenes`. El backend hace la cuenta real: descuenta insumos × cantidad de tandas, y suma al stock cantidad de tandas × rendimiento de la receta.
- **Proveedores** (`/proveedores`): alta/edición en un modal, contra `/api/proveedores`.
- **Compras** (`/compras`): elegís proveedor, cargás ítems (insumo, cantidad, costo unitario) y confirmás contra `POST /api/compras`.
- **Reportes** (`/reportes`): un selector de rango de fechas y cuatro pestañas con tablas simples — ventas por período (con desglose por día), productos más vendidos, margen por producto, y stock crítico.
- **Comisiones** (`/comisiones`, solo `DUENO`): mismo selector de rango de fechas, con tablas de comisión de vendedores (por turno) y de producción. La comisión de un vendedor se calcula sobre lo vendido **más** lo cobrado de cuenta corriente en ese turno — cobrar una deuda cuenta como gestión con el cliente, igual que una venta.
- **Auditoría** (`/auditoria`, solo `DUENO`): tabla con el historial de `GET /api/auditoria`, filtrable por entidad.
- **Usuarios** (`/usuarios`, solo `DUENO`): alta y edición de usuarios (nombre, email, rol, porcentaje de comisión, activo/inactivo y reseteo opcional de contraseña), mismo patrón modal que Clientes/Productos. Contra `POST`/`PUT /api/usuarios`.
- Un interceptor de Axios (`src/api/client.ts`) agrega el JWT a cada request y desloguea automáticamente ante un 401.

## Qué falta (fuera del alcance de esta etapa)

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
