# ERP Panadería

Ver [erp-panaderia-documento-diseno.md](erp-panaderia-documento-diseno.md) para el diseño completo (stack, arquitectura, módulos, modelo de datos, roadmap).

**Estado actual: Fase 0 (setup)** — proyecto Spring Boot, Postgres + Flyway vía Docker Compose, y módulo de autenticación (usuarios, roles, login JWT). Ver [backend/README.md](backend/README.md) para instrucciones de cómo levantar el entorno.

## Estructura

```
ERP/
├── docker-compose.yml   # Postgres para desarrollo local
├── .env / .env.example  # variables del docker-compose
└── backend/             # proyecto Spring Boot
```
