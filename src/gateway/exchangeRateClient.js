// Microservicio 3 (API externa de terceros): tipo de cambio de divisas con Frankfurter.
import { config } from '../config.js';
import { fetchJson, ServiceError } from './httpClient.js';

const SOURCE = 'Frankfurter External API';

export async function getExchangeRate(base = 'USD', target = 'MXN') {
  // Respuesta: { "amount": 1.0, "base": "USD", "date": "2026-10-28", "rates": { "MXN": 18.63 } }
  const data = await fetchJson(SOURCE, `${config.frankfurterUrl}/latest?from=${base}&to=${target}`);
  const rate = data?.rates?.[target];

  if (typeof rate !== 'number' || rate <= 0) {
    throw new ServiceError(SOURCE, `la respuesta no incluye la tasa ${base}-${target}`);
  }

  return { rate, source: SOURCE, date: data.date ?? null, cached: false };
}
