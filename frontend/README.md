# ERP Panadería — Frontend

React + Vite + TypeScript + Ant Design. Por ahora cubre lo mínimo para probar el flujo de punta a punta contra el backend: login y una pantalla de venta (POS) funcional, sin pulir.

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
- **POS** (`/pos`, ruta protegida): pantalla de venta.
  - Un input simula el lector láser: se tipea/escanea un código y Enter lo resuelve contra `GET /api/ventas/escanear` (distingue código de barras fijo de etiqueta de balanza PLU+peso) y lo agrega al carrito.
  - También se puede buscar un producto manualmente por nombre y agregarlo con una cantidad.
  - "Confirmar venta" llama a `POST /api/ventas`, que descuenta stock en el backend.
- Un interceptor de Axios (`src/api/client.ts`) agrega el JWT a cada request y desloguea automáticamente ante un 401.

## Qué falta (fuera del alcance de esta etapa)

- Pantallas de gestión de productos/inventario/caja (el backend ya tiene los endpoints).
- Pulido visual, manejo de sesión expirada más prolijo, tests de frontend.

## Estructura

```
src/
├── api/        # cliente Axios + funciones por recurso (auth, productos, ventas)
├── auth/       # AuthContext (JWT + usuario en localStorage) y ProtectedRoute
├── pages/      # LoginPage, PosPage
└── types/      # tipos TS que reflejan los DTOs del backend
```
