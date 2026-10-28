// Configuración centralizada. Todos los valores se pueden sobrescribir con variables de entorno.
const env = process.env;

const catalogPort = Number(env.CATALOG_PORT ?? 3001);
const inventoryPort = Number(env.INVENTORY_PORT ?? 3002);

export const config = {
  gatewayPort: Number(env.GATEWAY_PORT ?? 3000),
  catalogPort,
  inventoryPort,

  catalogUrl: env.CATALOG_URL ?? `http://localhost:${catalogPort}`,
  inventoryUrl: env.INVENTORY_URL ?? `http://localhost:${inventoryPort}`,

  frankfurterUrl: env.FRANKFURTER_URL ?? 'https://api.frankfurter.dev/v1',
  erApiUrl: env.ER_API_URL ?? 'https://open.er-api.com/v6',

  // Tiempo máximo que la Composite API espera a cada servicio
  serviceTimeoutMs: Number(env.SERVICE_TIMEOUT_MS ?? 3000),
  // Antigüedad máxima de la última tasa conocida para usarla como respaldo
  rateCacheMaxAgeMs: Number(env.RATE_CACHE_MAX_AGE_MS ?? 60 * 60 * 1000),
  // Retardo simulado del inventario (producto P-104) para demostrar el timeout
  simulatedDelayMs: Number(env.SIMULATED_DELAY_MS ?? 5000),
};
