# E-Commerce Gateway & Dashboard — Composite API

Práctica de **Desarrollo Web Integral – Unidad 3** (UTCH, Desarrollo y Gestión de Software).

Una **Composite API** (API agregadora) hecha con **Java 21, Spring Boot 3.5 y Maven** que, con **una sola petición del
cliente**, consulta en paralelo tres servicios y devuelve un único JSON consolidado con el detalle de un producto:

| # | Servicio | Tipo | Endpoint | Función |
|---|----------|------|----------|---------|
| 1 | Catálogo (`catalog-service`) | Propia | `GET /microservices/products/{id}` (puerto 8081) | Nombre, descripción, categoría y precio base en USD |
| 2 | Inventario (`inventory-service`) | Propia | `GET /microservices/inventory/{productId}` (puerto 8082) | Unidades disponibles, almacén y días de entrega |
| 3 | Divisas | Terceros | `https://api.frankfurter.dev/v1/latest?from=USD&to=MXN` (respaldo: `https://open.er-api.com/v6/latest/USD`) | Tipo de cambio USD → MXN en tiempo real |
| 4 | **Composite API** (`composite-api`) | Propia (agregadora) | `GET /api/v1/product-summary/{id}` (puerto 8080) | Orquesta las 3 peticiones y unifica la respuesta |

> El planteamiento menciona dos rutas para la API compuesta; ambas están disponibles:
> `/api/v1/product-summary/{id}` y `/api/v1/products/{id}/summary`.

## Arquitectura

```
                          ┌──────────────────────────────┐
  Navegador / Postman ──► │  composite-api  (:8080)      │
     1 sola petición      │  GET /api/v1/product-summary │
                          └──────┬──────────┬─────────┬──┘
           CompletableFuture + hilos virtuales (en paralelo, timeout 3 s c/u)
                                 │          │         │
                    ┌────────────▼──┐ ┌─────▼──────┐ ┌▼──────────────────────────┐
                    │ catalog-      │ │ inventory- │ │ Divisas (externa)         │
                    │ service :8081 │ │ service    │ │ Frankfurter → ER-API      │
                    └───────────────┘ │ :8082      │ └───────────────────────────┘
                                      └────────────┘
```

Cada servicio es una aplicación Spring Boot independiente (módulo Maven propio), por lo que se puede ejecutar,
desplegar y escalar por separado.

## Requisitos

- **JDK 21** o superior.
- Maven no es necesario: el proyecto incluye **Maven Wrapper** (`mvnw` / `mvnw.cmd`).
- Conexión a internet para la API de divisas (si no hay, la API responde igual con `PARTIAL_SUCCESS`).

## Instalación y ejecución

```bash
git clone <url-del-repositorio>
cd ecommerce-composite-api
./mvnw clean install          # Windows: mvnw.cmd clean install
```

Arrancar los 3 servicios, cada uno en su propia terminal:

```bash
./mvnw -pl catalog-service spring-boot:run      # http://localhost:8081
./mvnw -pl inventory-service spring-boot:run    # http://localhost:8082
./mvnw -pl composite-api spring-boot:run        # http://localhost:8080
```

En Windows también se puede usar **`start-all.cmd`**, que abre una ventana por servicio.

Alternativa con los JAR ya construidos:

```bash
java -jar catalog-service/target/catalog-service-1.0.0.jar
java -jar inventory-service/target/inventory-service-1.0.0.jar
java -jar composite-api/target/composite-api-1.0.0.jar
```

Abre **http://localhost:8080** para ver el dashboard web.

### Configuración

Cada módulo tiene su `src/main/resources/application.properties`. Los de la Composite API son:

| Propiedad | Valor por defecto | Descripción |
|-----------|-------------------|-------------|
| `services.catalog-url` | `http://localhost:8081` | URL del Catálogo |
| `services.inventory-url` | `http://localhost:8082` | URL del Inventario |
| `services.frankfurter-url` | `https://api.frankfurter.dev/v1` | API externa principal |
| `services.er-api-url` | `https://open.er-api.com/v6` | API externa de respaldo |
| `services.timeout` | `3s` | Tiempo máximo de espera por servicio |
| `services.rate-cache-max-age` | `1h` | Antigüedad máxima de la última tasa conocida |

## Ejemplo de respuesta

`GET http://localhost:8080/api/v1/product-summary/P-101`

```json
{
  "product_id": "P-101",
  "title": "Laptop Gamer XYZ",
  "description": "Laptop de 16\" con RTX 4070, 32 GB de RAM y SSD de 1 TB.",
  "category": "Computadoras",
  "price_usd": 1200.00,
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
| Un servicio tarda más de `services.timeout` | El `HttpClient` corta la petición y se trata como falla de ese servicio |
| Inventario no responde o da error | HTTP 200, `status: "PARTIAL_SUCCESS"`, `stock_status: "UNKNOWN"` y detalle en `errors` |
| Frankfurter no responde | Se consulta automáticamente **ExchangeRate-API** como respaldo |
| Ninguna API de divisas responde | Se usa la **última tasa conocida** (máx. 1 h); si no hay, `price_mxn: null` y `PARTIAL_SUCCESS` |
| El producto no existe | HTTP 404, `status: "NOT_FOUND"` |
| Catálogo caído (dato indispensable) | HTTP 503, `status: "ERROR"` |

## Pruebas

```bash
./mvnw test
```

- `catalog-service` y `inventory-service`: pruebas de los controladores con MockMvc.
- `composite-api`: 10 pruebas de integración que levantan un servidor HTTP falso en lugar de los microservicios y de
  las APIs de divisas, para comprobar cada escenario de la tabla anterior sin depender de internet.

Para las pruebas manuales hay una colección de Postman en
[`postman/Composite-API.postman_collection.json`](postman/Composite-API.postman_collection.json).

## Estructura del proyecto

```
├── pom.xml                         POM padre (multi-módulo)
├── catalog-service/                Microservicio 1: Catálogo (:8081)
├── inventory-service/              Microservicio 2: Inventario (:8082)
├── composite-api/                  Composite API + dashboard (:8080)
│   └── src/main/java/mx/utch/ecommerce/composite/
│       ├── config/                 RestClient con timeouts y executor de hilos virtuales
│       ├── client/                 Clientes HTTP de Catálogo, Inventario y Divisas
│       ├── service/                Orquestación y consolidación (ProductSummaryService)
│       ├── dto/                    Contrato JSON de respuesta
│       └── web/                    Controlador REST
├── postman/                        Colección de Postman
└── start-all.cmd                   Arranca los 3 servicios en Windows
```

## Flujo de trabajo Git (Gitflow)

- `main`: rama estable; contiene el commit inicial y recibe la versión final cuando se aprueba la entrega.
- `develop`: integración.
- `feature/*`: una rama por funcionalidad (`feature/catalog-service`, `feature/inventory-service`,
  `feature/composite-api`, `feature/resilience`, `feature/dashboard`, `feature/integration-tests`), fusionadas en
  `develop` con `--no-ff`.
- `release/1.0.0`: preparación de la entrega, fusionada de regreso en `develop`.
