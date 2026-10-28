// Microservicio 3 (API externa de terceros): tipo de cambio de divisas.
// Proveedor principal: Frankfurter. Respaldo: ExchangeRate-API. Último recurso: última tasa conocida.
import { config } from '../config.js';
import { fetchJson, ServiceError } from './httpClient.js';

const providers = [
  {
    name: 'Frankfurter External API',
    // Respuesta: { "amount": 1.0, "base": "USD", "date": "2026-10-28", "rates": { "MXN": 18.63 } }
    url: (base, target) => `${config.frankfurterUrl}/latest?from=${base}&to=${target}`,
    extract: (data, target) => ({ rate: data?.rates?.[target], date: data?.date }),
  },
  {
    name: 'ExchangeRate-API (open.er-api.com)',
    // Respuesta: { "result": "success", "base_code": "USD", "time_last_update_utc": "...", "rates": { "MXN": 18.64, ... } }
    url: (base) => `${config.erApiUrl}/latest/${base}`,
    extract: (data, target) => ({
      rate: data?.result === 'success' ? data.rates?.[target] : undefined,
      date: data?.time_last_update_utc,
    }),
  },
];

// Última tasa obtenida con éxito por par de monedas, p. ej. "USD-MXN".
const lastKnownRates = new Map();

export async function getExchangeRate(base = 'USD', target = 'MXN') {
  const pair = `${base}-${target}`;
  const failures = [];

  for (const provider of providers) {
    try {
      const data = await fetchJson(provider.name, provider.url(base, target));
      const { rate, date } = provider.extract(data, target);

      if (typeof rate !== 'number' || rate <= 0) {
        throw new ServiceError(provider.name, `la respuesta no incluye la tasa ${pair}`);
      }

      const result = { rate, source: provider.name, date: date ?? null, cached: false };
      lastKnownRates.set(pair, { ...result, fetchedAt: Date.now() });
      return result;
    } catch (err) {
      failures.push(`${err.service ?? provider.name}: ${err.message}`);
    }
  }

  // Ningún proveedor respondió: usar la última tasa conocida si no es demasiado antigua.
  const cached = lastKnownRates.get(pair);
  if (cached && Date.now() - cached.fetchedAt <= config.rateCacheMaxAgeMs) {
    return { rate: cached.rate, source: cached.source, date: cached.date, cached: true };
  }

  throw new ServiceError('Divisas', failures.join(' | '));
}
