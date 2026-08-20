# ERP Panadería

Ver [erp-panaderia-documento-diseno.md](erp-panaderia-documento-diseno.md) para el diseño completo (stack, arquitectura, módulos, modelo de datos, roadmap).

**Estado actual: Fase 1 (MVP funcional)** — Fase 0 (setup, auth JWT) completa, y Fase 1 en curso: Productos, Inventario, Caja y Ventas (POS) en el backend, con un frontend funcional (login + pantalla de venta) para probar el flujo de punta a punta. Ver [backend/README.md](backend/README.md) y [frontend/README.md](frontend/README.md).

## Estructura

```
ERP/
├── docker-compose.yml   # Postgres para desarrollo local
├── .env / .env.example  # variables del docker-compose
├── backend/              # proyecto Spring Boot
└── frontend/             # proyecto React + Vite + TypeScript
```

## Levantar todo el stack en desarrollo

```bash
docker compose up -d          # Postgres
cd backend && ./mvnw spring-boot:run   # API en :8080
cd frontend && npm install && npm run dev   # UI en :5173
```
