# ERP Panadería — Frontend

React + Vite + TypeScript + Ant Design. Cubre lo mínimo para probar de punta a punta lo que hay en el backend: login, venta (POS) con cuenta corriente, recetas, producción, proveedores, compras, reportes y comisiones — funcional, sin pulir.

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
- **`AppLayout`**: header con navegación compartido por todas las pantallas autenticadas, filtrado por rol igual que el backend (`VENDEDOR` solo ve "Venta"; "Comisiones" es exclusivo de `DUENO`).
- **POS** (`/pos`): pantalla de venta.
  - Un input simula el lector láser: se tipea/escanea un código y Enter (o el botón "Agregar") lo resuelve contra `GET /api/ventas/escanear` (distingue código de barras fijo de etiqueta de balanza PLU+peso) y lo agrega al carrito.
  - También se puede buscar un producto manualmente por nombre y agregarlo con una cantidad.
  - Si el medio de pago es "Cuenta corriente" aparece un selector de cliente (solo lista los que tienen `tieneCuentaCorriente = true`) y no deja confirmar sin elegir uno.
  - "Confirmar venta" llama a `POST /api/ventas`, que descuenta stock en el backend.
- **Recetas** (`/recetas`): elegís un producto elaborado, cargás/editás los ítems (insumo + cantidad por unidad de producto) y guardás contra `POST`/`PUT /api/produccion/recetas`.
- **Producción** (`/produccion`): elegís un producto con receta, ponés la cantidad a producir, ves una preview de cuánto insumo se va a consumir (calculada en el cliente a partir de la receta) y confirmás contra `POST /api/produccion/ordenes`.
- **Proveedores** (`/proveedores`): alta/edición en un modal, contra `/api/proveedores`.
- **Compras** (`/compras`): elegís proveedor, cargás ítems (insumo, cantidad, costo unitario) y confirmás contra `POST /api/compras`.
- **Reportes** (`/reportes`): un selector de rango de fechas y cuatro pestañas con tablas simples — ventas por período (con desglose por día), productos más vendidos, margen por producto, y stock crítico.
- **Comisiones** (`/comisiones`, solo `DUENO`): mismo selector de rango de fechas, con tablas de comisión de vendedores (por turno) y de producción.
- Un interceptor de Axios (`src/api/client.ts`) agrega el JWT a cada request y desloguea automáticamente ante un 401.

## Qué falta (fuera del alcance de esta etapa)

- Pantalla para dar de alta insumos y clientes: Recetas/Compras (insumos) y el selector de cuenta corriente en el POS (clientes) asumen que ya existen, creados directo contra la API (no hay UI para eso todavía).
- Pantallas de gestión de productos/categorías/inventario/caja (el backend ya tiene los endpoints).
- Pantalla para registrar pagos de cuenta corriente y ver el saldo de un cliente (el backend ya expone `GET /api/clientes/{id}/saldo` y `POST /api/clientes/{id}/pagos`).
- Pulido visual, manejo de sesión expirada más prolijo, tests de frontend.

## Verificado de punta a punta

Con Docker ya funcionando se pudo probar contra un backend real (no solo simulado): login, escaneo de código de barras en el POS, una venta a cuenta corriente completa (con verificación de que el saldo del cliente se actualiza bien), y los reportes de ventas/comisiones mostrando datos reales de esas ventas. Esto sacó a la luz un bug real que quedó corregido: faltaba configurar CORS en el backend (el browser bloqueaba todas las requests reales del frontend aunque el preflight `OPTIONS` pasara con 200) — nunca se había detectado porque hasta ahora el frontend solo se había probado sin un backend real corriendo en paralelo.

## Estructura

```
src/
├── api/        # cliente Axios + funciones por recurso (auth, productos, ventas, inventario, produccion, compras, clientes, comisiones, reportes)
├── auth/       # AuthContext (JWT + usuario en localStorage) y ProtectedRoute
├── layout/     # AppLayout: header + navegación compartida por rol
├── pages/      # LoginPage, PosPage, RecetasPage, ProduccionPage, ProveedoresPage, ComprasPage, ReportesPage, ComisionesPage
└── types/      # tipos TS que reflejan los DTOs del backend
```
