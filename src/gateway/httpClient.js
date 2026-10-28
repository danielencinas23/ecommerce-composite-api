// Cliente HTTP común para todas las llamadas salientes de la Composite API.
// Usa fetch nativo de Node.js con timeout y convierte cualquier falla en un ServiceError.
import { config } from '../config.js';

export class ServiceError extends Error {
  constructor(service, message, { status = null, cause } = {}) {
    super(message, { cause });
    this.name = 'ServiceError';
    this.service = service;
    this.status = status; // código HTTP devuelto por el servicio (null si no hubo respuesta)
  }
}

export async function fetchJson(service, url, { timeoutMs = config.serviceTimeoutMs } = {}) {
  let response;
  try {
    response = await fetch(url, {
      headers: { Accept: 'application/json' },
      signal: AbortSignal.timeout(timeoutMs),
    });
  } catch (err) {
    const reason = err.name === 'TimeoutError' ? `sin respuesta tras ${timeoutMs} ms (timeout)` : 'servicio no disponible';
    throw new ServiceError(service, reason, { cause: err });
  }

  if (!response.ok) {
    throw new ServiceError(service, `respondió HTTP ${response.status}`, { status: response.status });
  }

  try {
    return await response.json();
  } catch (err) {
    throw new ServiceError(service, 'respuesta JSON inválida', { cause: err });
  }
}
