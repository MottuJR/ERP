# Documento de Diseño — ERP para Panadería

**Autor:** Mateo
**Fecha:** Agosto 2026
**Estado:** Borrador inicial — base para desarrollo con Claude Code

---

## 1. Objetivo del proyecto

Construir un sistema tipo ERP para gestionar una panadería de punto a punto: producción, inventario de insumos y productos, ventas en mostrador (POS), compras a proveedores, caja y reportes básicos.

El foco inicial es resolver bien el caso real de la panadería. En paralelo, el modelo de datos y la arquitectura se diseñan para que en el futuro sea relativamente simple agregar o quitar módulos y adaptar el sistema a otro tipo de comercio (kiosco, almacén, etc.), sin que eso implique reescribir el sistema desde cero. Esto es una consideración de diseño, no un requisito a implementar ahora.

---

## 2. Stack tecnológico

| Capa | Tecnología | Motivo |
|---|---|---|
| Backend | Java 21 (LTS) + Spring Boot 4.0.x | LTS con soporte y tooling maduro, compatible con Spring Boot 4 (que requiere Java 21 como mínimo). Alta demanda laboral. |
| Persistencia | Spring Data JPA + Hibernate | Estándar de facto en el ecosistema Spring, mapea bien un dominio transaccional como este. |
| Base de datos | PostgreSQL | Buen soporte de tipos, robusto para reportes y transacciones, gratis y ampliamente usado en producción. |
| Migraciones | Flyway | Versiona el esquema de la base desde el día 1; fundamental si en el futuro hay que migrar o replicar el sistema para otro cliente. |
| Seguridad | Spring Security + JWT | La API queda stateless, separada del frontend, con roles (dueño, encargado, vendedor). |
| Frontend | React + TypeScript | Mejor experiencia para pantallas de venta en tiempo real (carrito, búsqueda rápida) que un render server-side. Combinación muy pedida junto con Java/Spring. |
| UI Kit | Ant Design o Mantine | Componentes ya armados de tablas, formularios y dashboards — cubre gran parte de lo que necesita un ERP sin reinventar CSS. |
| Testing backend | JUnit 5 + Mockito + Testcontainers | Testcontainers permite testear contra un Postgres real en Docker, evitando sorpresas de compatibilidad con H2. |
| Contenedores | Docker + docker-compose | Para levantar backend + base de datos de forma reproducible en desarrollo, y facilitar el deploy más adelante. |

> Nota sobre versiones: Spring Boot 4.0 salió en noviembre de 2025 y requiere como mínimo Java 21. Java 25 es la LTS más reciente (septiembre 2025), pero Java 21 tiene tooling y documentación más maduros por ahora, así que es la opción más práctica para aprender. Si en unos meses el ecosistema ya giró hacia Java 25 sin fricciones, migrar es sencillo.

**Fuentes de referencia (versiones, agosto 2026):**
- [Spring Boot 4.0.0 available now](https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now/)
- [Spring Boot 4.0.0 and Java 21: What You Need to Know](https://docs.bswen.com/blog/2026-02-28-springboot-400-java21-support/)
- [Oracle Releases Java 25](https://www.oracle.com/news/announcement/oracle-releases-java-25-2025-09-16/)
- [Spring Boot version history](https://www.codejava.net/frameworks/spring-boot/spring-boot-version-history)

---

## 3. Arquitectura general

Arrancar con un **monolito modular**, no microservicios. Con un solo desarrollador aprendiendo el stack, la complejidad operativa de microservicios (orquestación, comunicación entre servicios, despliegues separados) no aporta valor todavía y sí resta tiempo de desarrollo.

Lo importante para la meta de "poder agregar o quitar módulos" no es tanto separar servicios, sino organizar el código **por funcionalidad (feature) y no por capa técnica**. En vez de tener un paquete `controllers`, otro `services`, otro `repositories` con todo mezclado, cada módulo de negocio es un paquete autocontenido:

```
com.panaderia.erp
├── core/            # entidades y utilidades compartidas (Usuario, Rol, auditoría)
├── auth/            # login, JWT, seguridad
├── productos/        # Producto, Categoria
├── inventario/        # Insumo, MovimientoStock
├── produccion/        # Receta, OrdenProduccion
├── ventas/            # Venta, DetalleVenta, Cliente
├── compras/           # Compra, DetalleCompra, Proveedor
├── caja/              # Caja, MovimientoCaja
└── reportes/          # consultas y agregaciones de solo lectura
```

Cada paquete tiene sus propios `Controller`, `Service`, `Repository` y entidades. La comunicación entre módulos se hace a través de interfaces de servicio (por ejemplo, `ventas` le pide a `inventario` que descuente stock, no accede directamente a su repositorio). Esto es lo que en el futuro permite "apagar" un módulo (como `produccion`, si un kiosco no fabrica nada) sin tocar el resto del sistema.

La API es REST, stateless, consumida por el frontend React vía JSON. Autenticación con JWT en el header `Authorization`.

---

## 4. Módulos funcionales

**Productos:** alta/baja de productos, categorías, si un producto es elaborado (tiene receta) o de reventa directa (se compra ya terminado), precio de venta.

**Inventario:** insumos (materia prima) con stock y unidad de medida, alertas de stock mínimo, historial de movimientos (entradas, salidas, ajustes, mermas) para trazabilidad.

**Producción:** recetas que vinculan un producto elaborado con los insumos y cantidades que consume, órdenes de producción que al confirmarse descuentan insumos y suman stock de producto terminado, registro de mermas.

**Ventas / POS:** pantalla de venta rápida, carrito, medios de pago, al confirmar la venta descuenta stock de producto automáticamente y registra el movimiento correspondiente. Soporta lectura con lector láser: tanto de productos con código de barras fijo como de etiquetas de balanza para productos vendidos a peso (ver nota en sección 5).

**Compras:** proveedores, órdenes de compra, al recibir mercadería se suma stock de insumos y se actualiza el costo.

**Caja:** apertura y cierre de caja por turno, registro de ingresos/egresos manuales, arqueo (comparar lo esperado contra lo contado).

**Reportes:** ventas por período, productos más vendidos, margen por producto (precio de venta vs. costo de insumos según receta), stock crítico.

**Usuarios y roles:** dueño (acceso total), encargado (gestión operativa, sin configuración sensible), vendedor (solo POS y consulta de stock).

---

## 5. Modelo de datos (entidades principales)

| Entidad | Campos clave | Relaciones |
|---|---|---|
| `Usuario` | id, nombre, email, passwordHash, rol, activo | — |
| `Producto` | id, nombre, categoriaId, tipo (ELABORADO / REVENTA), **seVendePorPeso**, precioVenta, unidadMedida, **codigoBarras (nullable)**, **codigoPLU (nullable)**, stockActual, stockMinimo, activo | pertenece a `Categoria`, opcionalmente tiene `Receta` |
| `Categoria` | id, nombre | — |
| `Insumo` | id, nombre, unidadMedida, stockActual, stockMinimo, costoUnitario | — |
| `Receta` | id, productoId, items (insumoId + cantidad) | vincula `Producto` con `Insumo` |
| `OrdenProduccion` | id, productoId, cantidad, fecha, estado, usuarioId | referencia `Producto` |
| `Cliente` | id, nombre, telefono, tieneCuentaCorriente | opcional, para etapas futuras |
| `Proveedor` | id, nombre, contacto, telefono, email | — |
| `Venta` | id, fecha, clienteId (nullable), usuarioId, **cajaId (nullable)**, total, medioPago, estado | tiene muchos `DetalleVenta`, referencia `Caja` |
| `DetalleVenta` | id, ventaId, productoId, cantidad, precioUnitario, subtotal | referencia `Venta` y `Producto` |
| `Compra` | id, proveedorId, fecha, total, estado | tiene muchos `DetalleCompra` |
| `DetalleCompra` | id, compraId, insumoId, cantidad, costoUnitario, subtotal | referencia `Compra` e `Insumo` |
| `MovimientoStock` | id, tipo (ENTRADA/SALIDA/AJUSTE/MERMA), itemTipo (PRODUCTO/INSUMO), itemId, cantidad, fecha, motivo, referenciaId | trazabilidad de todo cambio de stock |
| `Caja` | id, fechaApertura, fechaCierre, montoInicial, montoFinal, usuarioId, estado | tiene muchos `MovimientoCaja` |
| `MovimientoCaja` | id, cajaId, tipo (INGRESO/EGRESO), monto, concepto, fecha | referencia `Caja` |

**Nota: código de barras y productos vendidos a peso.** No es un solo campo — hay dos casos. Los productos con código fijo (reventa empaquetada) usan `codigoBarras`, un valor estático que se busca tal cual. Los productos que se venden a peso (pan, facturas sueltas) usan `codigoPLU`, un código corto interno.

**Decisión: la balanza trabaja en "modo peso", no en "modo precio".** Para evitar cargar el precio dos veces (en el sistema y en la balanza), la balanza se configura para imprimir el código con el PLU y el peso solamente — sin precio. En la balanza solo se carga el PLU y el nombre del producto (para que la etiqueta sea legible), nunca el precio. El precio vive únicamente en `Producto.precioVenta` (precio por kilo, para estos productos), dentro del sistema. Al escanear el código en el POS: se identifica el prefijo de "peso variable" (en Argentina, típicamente el rango 20-29, reservado para uso interno de comercio), se separan PLU y peso, se busca `Producto` por `codigoPLU`, y se calcula `total = peso (del código) × precioVenta (del sistema)`. El reparto exacto de dígitos entre PLU y peso dentro del código varía según el fabricante de la balanza, así que ese detalle de parseo se confirma contra el manual del modelo elegido al implementarlo.

Se descartó, para el MVP, conectar la balanza directamente al sistema (que lea el peso en vivo por cable): es una integración más frágil, atada a un protocolo propietario por marca, y con el modo peso ya se logra el mismo objetivo (no cargar precio dos veces) sin esa complejidad. Queda como posible mejora futura si el volumen de venta lo justifica.

El lector láser en sí no requiere integración especial: funciona como teclado (HID), no necesita driver ni SDK.

**Por qué este modelo escala bien a futuro:** ninguna entidad está atada al rubro "panadería" específicamente. `Receta` es opcional (un kiosco puede no usarla), `Insumo` y `Producto` son genéricos, y `MovimientoStock` centraliza la trazabilidad sin importar el origen (venta, compra, producción o ajuste manual). Adaptar el sistema a otro comercio, en el futuro, sería más una cuestión de qué módulos mostrar/ocultar que de rediseñar entidades.

---

## 6. Roadmap sugerido

**Fase 0 — Setup:** proyecto Spring Boot base, conexión a Postgres, Flyway configurado, autenticación con JWT y roles básicos.

**Fase 1 — MVP funcional:** módulos de Productos, Inventario básico y Ventas (POS simple con descuento automático de stock) y Caja. Con esto la panadería ya podría empezar a vender registrando todo.

**Fase 2 — Producción y Compras:** recetas, órdenes de producción con consumo de insumos, proveedores y compras que reponen stock de insumos.

**Fase 3 — Reportes:** ventas por período, productos más vendidos, márgenes, alertas de stock. Incluye eventualmente el módulo de comisiones/liquidaciones (ver sección 7).

**Fase 4 — Pulido:** permisos más finos por rol, auditoría de cambios, mejoras de UX en el POS.

**Fase 5 (futuro, no ahora):** evaluar si conviene generalizar a otros rubros — eventualmente esto podría significar hacer configurable qué módulos están activos por instalación, o incluso una arquitectura multi-tenant si se llega a ofrecer como SaaS a varios comercios a la vez.

---

## 7. Regla de negocio diferida: comisiones

Esta funcionalidad **no se implementa en el MVP**, pero se documenta acá porque condiciona pequeños detalles del modelo de datos. La idea es que el sistema pueda calcular comisiones de dos tipos de empleados con reglas distintas:

**Vendedores:** cobran un porcentaje sobre el total vendido **por turno** (no por día calendario). Un turno equivale a una `Caja` abierta y cerrada. La fórmula sería, por cada `Caja`: `comisión = suma(Venta.total de ese usuario en ese turno) × porcentaje del vendedor`.

**Empleados de producción:** cobran un porcentaje calculado sobre el precio del producto que fabrican, en relación a la cantidad que realizan. La fórmula por `OrdenProduccion` sería: `comisión = cantidad producida × precio del producto × porcentaje del empleado`.

**Por qué se agregó `cajaId` a `Venta` ahora, aunque el cálculo se haga después:** para poder agrupar ventas por turno de forma directa y sin ambigüedad. La alternativa sería inferir el turno cruzando la fecha de la venta contra el rango de apertura/cierre de cada `Caja`, lo cual es más frágil (turnos que se superponen, cierres tardíos, etc.). Agregar la columna ahora es gratis; sacar esta lógica de una fecha cruzada después no lo es.

**Decisiones que quedan pendientes para cuando se implemente** (no bloquean nada del MVP):

- Dónde vive el porcentaje de comisión: como campo en `Usuario` (por ejemplo `porcentajeComision`), o en una tabla de configuración aparte si en algún momento el porcentaje varía por producto o por período.
- Si "precio del producto" para la comisión de producción es `Producto.precioVenta` o un valor distinto (por ejemplo un costo de referencia interno).
- Si las comisiones se calculan on-demand (reporte) o se registran como movimientos concretos (por ejemplo una tabla `Liquidacion` con el detalle de cada período pagado).

El lugar natural para esto en el roadmap es un módulo nuevo, `comisiones` o `liquidaciones`, dentro de la Fase 3/4, que solo lee de `Venta`, `OrdenProduccion` y `Caja` sin modificarlas.

---

## 8. Regla de negocio diferida: cuentas corrientes de clientes

Objetivo: que un cliente pueda llevarse productos "a cuenta" (fiado) y que el sistema sepa no solo cuánto debe en total, sino qué es específicamente lo que debe.

A diferencia de las comisiones, esto **prácticamente no requiere cambios en el modelo actual**. `Venta` ya tiene `clienteId` y `medioPago`, y cada venta ya guarda su detalle completo en `DetalleVenta` (productos, cantidades, precios). Alcanza con que `medioPago` acepte el valor `CUENTA_CORRIENTE` para que quede registrado, de cada venta a cuenta, exactamente qué se llevó el cliente — no solo un monto acumulado.

Lo único que falta agregar, más adelante, es cómo registrar cuando el cliente paga esa deuda (total o parcialmente):

- **`PagoCliente`** (nueva entidad, independiente de todo lo existente): id, clienteId, fecha, monto, medioPago.
- **Cálculo del saldo:** `saldo = suma(Venta.total donde medioPago = CUENTA_CORRIENTE, para ese cliente) − suma(PagoCliente.monto, para ese cliente)`.

Como es una tabla nueva que no modifica ninguna entidad existente, se puede sumar en cualquier fase posterior (naturalmente encajaría junto con el módulo de Ventas ampliado, o como parte de la Fase 2) sin ningún costo de migración retroactiva. Lo único que sí conviene tener presente desde ahora es habilitar `CUENTA_CORRIENTE` como medio de pago válido en `Venta` para no perder ese dato en las ventas que se hagan mientras tanto.

Decisión pendiente para cuando se implemente: si se necesita un límite de cuenta corriente por cliente (`Cliente.limiteCuentaCorriente`), para bloquear nuevas ventas a cuenta si el saldo supera un tope. No es necesario para el MVP de esta funcionalidad, pero es fácil de agregar como columna simple cuando llegue el momento.

---

## 9. Infraestructura y despliegue

**Decisión: hosting en la nube (VPS), no servidor local en el negocio.** Ya definimos que el sistema necesita internet para funcionar y que a futuro se quiere ofrecer a otros comercios, así que tiene sentido alojarlo de forma centralizada en vez de depender de una PC física en cada local. El negocio accede desde cualquier dispositivo con navegador (PC, tablet, celular) apuntando a la URL del sistema — no instala nada.

**Proveedor:** cualquier VPS chico alcanza para un solo comercio (por ejemplo Hetzner o DigitalOcean, del orden de unos pocos dólares por mes). Si se prefiere no administrar el servidor a mano, plataformas más "gestionadas" como Railway o Render simplifican el deploy a costa de un poco menos de control — válido también para arrancar, sobre todo mientras se está aprendiendo.

**Cómo se despliega:** el mismo `docker-compose` pensado para desarrollo local se extiende para producción, agregando un proxy reverso (Nginx o Caddy) delante del backend. Caddy en particular simplifica bastante esto porque gestiona automáticamente el certificado HTTPS (vía Let's Encrypt) sin configuración manual — conviene para no perder tiempo en eso al principio.

**Dominio y HTTPS:** hace falta comprar un dominio (o subdominio si ya se tiene uno) y que el sistema quede detrás de HTTPS, ya que se manejan datos de ventas, clientes y contraseñas — no es opcional aunque sea un solo comercio chico.

**Backups:** copias periódicas de la base (`pg_dump` programado, por ejemplo con un cron dentro del VPS) subidas a un almacenamiento externo (S3 o similar), para no depender de que el disco del servidor nunca falle. Esto es fácil de posponer un poco, pero no debería quedar afuera de la Fase 1 — es la fase donde ya hay datos reales de ventas que no se quieren perder.

**Configuración por entorno:** las contraseñas de la base y la clave de firma de JWT nunca van escritas en el código — se cargan como variables de entorno, distintas en desarrollo y en producción (`application-dev.yml` vs `application-prod.yml` en Spring Boot).

**Hardware en el mostrador — lo que falta definir:** ya cubrimos el lector láser (funciona como teclado, sin integración especial) y la balanza (modo peso, código con PLU + peso). Queda pendiente la impresora de tickets/recibos — lo más simple y compatible es una impresora térmica USB que el sistema operativo reconoce como impresora estándar, y el navegador le imprime el comprobante directamente (`window.print()` con una plantilla angosta tipo ticket). No es necesario resolverlo ahora; encaja bien recién cuando se construya la pantalla de POS en la Fase 1.

**Dónde entra esto en el roadmap:** tener un VPS básico con HTTPS funcionando conviene armarlo temprano (Fase 0 o al cierre de la Fase 1), para probar el sistema en un entorno real cuanto antes en vez de descubrir problemas de despliegue recién al final. Backups automáticos y monitoreo más fino pueden esperar a la Fase 4.

---

## 10. Próximos pasos

Este documento sirve como punto de partida para generar el proyecto con Claude Code (o cualquier otra herramienta de desarrollo) en tu máquina, con git y base de datos local. El primer paso técnico concreto sería:

1. Crear el proyecto Spring Boot (via Spring Initializr) con las dependencias: Spring Web, Spring Data JPA, Spring Security, PostgreSQL Driver, Flyway, Validation.
2. Armar el `docker-compose.yml` con el servicio de Postgres para desarrollo local.
3. Implementar el módulo `core` y `auth` (usuarios, roles, login con JWT).
4. Implementar el módulo `productos` e `inventario` como primer caso de uso completo (entidad → repositorio → servicio → controlador → test).
5. A partir de ahí, repetir el patrón para cada módulo de la Fase 1.
6. Cuando la Fase 1 esté cerca de cerrar, armar el VPS con Docker + Caddy y hacer el primer deploy real (ver sección 9).
