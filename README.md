# E-Commerce Gateway & Dashboard — Composite API

Práctica de **Desarrollo Web Integral – Unidad 3** (UTCH, Desarrollo y Gestión de Software).

Una **Composite API** (API agregadora) que, con **una sola petición del cliente**, consulta en paralelo tres servicios y
devuelve un único JSON consolidado con la información del detalle de un producto:

| # | Servicio | Tipo | Endpoint | Función |
|---|----------|------|----------|---------|
| 1 | Catálogo | Propia | `GET /microservices/products/{id}` (puerto 3001) | Nombre, descripción, categoría y precio base en USD |
| 2 | Inventario | Propia | `GET /microservices/inventory/{productId}` (puerto 3002) | Unidades disponibles, almacén y días de entrega |
| 3 | Divisas | Terceros | `https://api.frankfurter.dev/v1/latest?from=USD&to=MXN` (respaldo: `https://open.er-api.com/v6/latest/USD`) | Tipo de cambio USD → MXN en tiempo real |
| 4 | **Composite API** | Propia (agregadora) | `GET /api/v1/product-summary/{id}` (puerto 3000) | Orquesta las 3 peticiones y unifica la respuesta |

> El planteamiento menciona dos rutas para la API compuesta; ambas están disponibles:
> `/api/v1/product-summary/{id}` y `/api/v1/products/{id}/summary`.

## Arquitectura

```
                          ┌──────────────────────────────┐
  Navegador / Postman ──► │  Composite API  (:3000)      │
     1 sola petición      │  GET /api/v1/product-summary │
                          └──────┬──────────┬─────────┬──┘
                Promise.allSettled (en paralelo, timeout 3 s c/u)
                                 │          │         │
                    ┌────────────▼──┐ ┌─────▼──────┐ ┌▼──────────────────────────┐
                    │ Catálogo      │ │ Inventario │ │ Divisas (externa)         │
                    │ :3001         │ │ :3002      │ │ Frankfurter → ER-API      │
                    └───────────────┘ └────────────┘ └───────────────────────────┘
```

## Requisitos

- Node.js 20 o superior (se usa `fetch` nativo y `AbortSignal.timeout`).
- Conexión a internet para la API de divisas (si no hay, la API responde igual con `PARTIAL_SUCCESS`).

## Instalación y ejecución

```bash
git clone <url-del-repositorio>
cd ecommerce-composite-api
npm install
npm start
```

`npm start` levanta los tres servidores, cada uno en su puerto:

```
[Catálogo]   http://localhost:3001/microservices/products/{id}
[Inventario] http://localhost:3002/microservices/inventory/{productId}
[Gateway]    http://localhost:3000/api/v1/product-summary/{id}
[Dashboard]  http://localhost:3000
```

También se pueden ejecutar por separado (cada uno en su propia terminal):

```bash
npm run start:catalog
npm run start:inventory
npm run start:gateway
```

Abre **http://localhost:3000** para ver el dashboard web.

### Variables de entorno (opcionales)

Ver [`.env.example`](.env.example). Las principales son `GATEWAY_PORT`, `CATALOG_URL`, `INVENTORY_URL` y
`SERVICE_TIMEOUT_MS` (por defecto 3000 ms).

## Ejemplo de respuesta

`GET http://localhost:3000/api/v1/product-summary/P-101`

```json
{
  "product_id": "P-101",
  "title": "Laptop Gamer XYZ",
  "description": "Laptop de 16\" con RTX 4070, 32 GB de RAM y SSD de 1 TB.",
  "category": "Computadoras",
  "price_usd": 1200,
  "price_mxn": 22357.32,
  "exchange_rate_usd_mxn": 18.6311,
  "stock_status": "IN_STOCK",
  "available_units": 15,
  "warehouse": "CUU-01 - Chihuahua, Chih.",
  "estimated_delivery_days": 2,
  "currency_conversion_source": "Frankfurter External API",
  "status": "SUCCESS"
}
```

## Productos de prueba

| ID | Escenario | Resultado esperado |
|----|-----------|--------------------|
| `P-101` | Todo responde | `SUCCESS`, `IN_STOCK` |
| `P-102` | Pocas unidades (≤ 5) | `SUCCESS`, `LOW_STOCK` |
| `P-103` | 0 unidades | `SUCCESS`, `OUT_OF_STOCK` |
| `P-104` | El inventario tarda 5 s (más que el timeout) | `PARTIAL_SUCCESS`, stock `UNKNOWN` |
| `P-105` | El inventario responde HTTP 500 | `PARTIAL_SUCCESS`, stock `UNKNOWN` |
| `P-999` | No existe en el catálogo | HTTP 404, `NOT_FOUND` |

## Manejo de errores y resiliencia

| Falla | Comportamiento de la Composite API |
|-------|------------------------------------|
| Un servicio tarda más de `SERVICE_TIMEOUT_MS` | `AbortSignal.timeout` cancela la petición y se trata como falla |
| Inventario no responde o da error | HTTP 200, `status: "PARTIAL_SUCCESS"`, `stock_status: "UNKNOWN"` y detalle en `errors` |
| Frankfurter no responde | Se consulta automáticamente **ExchangeRate-API** como respaldo |
| Ninguna API de divisas responde | Se usa la **última tasa conocida** (máx. 1 h); si no hay, `price_mxn: null` y `PARTIAL_SUCCESS` |
| El producto no existe | HTTP 404, `status: "NOT_FOUND"` |
| Catálogo caído (dato indispensable) | HTTP 503, `status: "ERROR"` |

## Pruebas

```bash
npm test
```

Pruebas de integración con `node:test` que levantan los microservicios y simulan las APIs de divisas para comprobar
cada escenario de la tabla anterior (sin depender de internet).

Para las pruebas manuales hay una colección de Postman en
[`postman/Composite-API.postman_collection.json`](postman/Composite-API.postman_collection.json).

## Estructura del proyecto

```
├── public/                      Dashboard web (HTML, CSS y JS sin frameworks)
├── postman/                     Colección de Postman
├── src/
│   ├── config.js                Configuración (puertos, URLs, timeouts)
│   ├── index.js                 Arranca los 3 servidores
│   ├── services/
│   │   ├── catalog/             Microservicio 1: Catálogo
│   │   └── inventory/           Microservicio 2: Inventario
│   └── gateway/
│       ├── server.js            Composite API (rutas y códigos HTTP)
│       ├── productSummaryService.js  Orquestación y consolidación
│       ├── exchangeRateClient.js     Cliente de la API externa de divisas
│       └── httpClient.js        fetch con timeout y manejo de errores
└── test/                        Pruebas de integración
```

## Flujo de trabajo Git (Gitflow)

- `main`: versiones entregables.
- `develop`: integración.
- `feature/*`: una rama por funcionalidad (`feature/catalog-service`, `feature/inventory-service`,
  `feature/composite-api`, `feature/dashboard`, `feature/integration-tests`), fusionadas en `develop` con `--no-ff`.
- `release/1.0.0`: preparación de la entrega, fusionada en `main` y etiquetada `v1.0.0`.
