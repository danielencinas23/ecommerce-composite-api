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

  // Tiempo máximo que la Composite API espera a cada servicio
  serviceTimeoutMs: Number(env.SERVICE_TIMEOUT_MS ?? 3000),
};
