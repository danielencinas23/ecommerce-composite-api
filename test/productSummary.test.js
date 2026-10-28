// Pruebas de integración de la Composite API. Levantan los microservicios reales
// y sustituyen las APIs de divisas por un servidor falso para controlar sus fallas.
import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';

const PORTS = { gateway: 4100, catalog: 4101, inventory: 4102, fx: 4103 };

process.env.CATALOG_URL = `http://localhost:${PORTS.catalog}`;
process.env.INVENTORY_URL = `http://localhost:${PORTS.inventory}`;
process.env.FRANKFURTER_URL = `http://localhost:${PORTS.fx}/frankfurter`;
process.env.ER_API_URL = `http://localhost:${PORTS.fx}/er`;
process.env.SERVICE_TIMEOUT_MS = '300';
process.env.SIMULATED_DELAY_MS = '600';

// Estado de las APIs de divisas falsas
const fx = { frankfurterUp: true, erUp: true };

const fxServer = http.createServer((req, res) => {
  const send = (status, body) => {
    res.writeHead(status, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(body));
  };
  if (req.url.startsWith('/frankfurter/latest')) {
    return fx.frankfurterUp
      ? send(200, { amount: 1, base: 'USD', date: '2026-10-28', rates: { MXN: 20 } })
      : send(503, { message: 'down' });
  }
  if (req.url.startsWith('/er/latest/USD')) {
    return fx.erUp
      ? send(200, { result: 'success', time_last_update_utc: 'Wed, 28 Oct 2026', rates: { MXN: 19.5, EUR: 0.9 } })
      : send(503, { result: 'error' });
  }
  send(404, {});
});

const servers = {};

async function getSummary(id) {
  const res = await fetch(`http://localhost:${PORTS.gateway}/api/v1/product-summary/${id}`);
  return { status: res.status, body: await res.json() };
}

function listen(server, port) {
  return new Promise((resolve) => server.listen(port, resolve));
}

function close(server) {
  server.closeAllConnections();
  return new Promise((resolve) => server.close(resolve));
}

before(async () => {
  // Importación dinámica: config.js debe leer las variables de entorno definidas arriba.
  const { createCatalogApp } = await import('../src/services/catalog/server.js');
  const { createInventoryApp } = await import('../src/services/inventory/server.js');
  const { createGatewayApp } = await import('../src/gateway/server.js');

  servers.catalog = http.createServer(createCatalogApp());
  servers.inventory = http.createServer(createInventoryApp());
  servers.gateway = http.createServer(createGatewayApp());

  await Promise.all([
    listen(fxServer, PORTS.fx),
    listen(servers.catalog, PORTS.catalog),
    listen(servers.inventory, PORTS.inventory),
    listen(servers.gateway, PORTS.gateway),
  ]);
});

after(async () => {
  await Promise.all([fxServer, ...Object.values(servers)].filter((s) => s.listening).map(close));
});

test('sin ninguna API de divisas ni tasa previa: precio MXN nulo y PARTIAL_SUCCESS', async () => {
  fx.frankfurterUp = false;
  fx.erUp = false;
  const { status, body } = await getSummary('P-101');

  assert.equal(status, 200);
  assert.equal(body.status, 'PARTIAL_SUCCESS');
  assert.equal(body.price_mxn, null);
  assert.equal(body.currency_conversion_source, 'UNAVAILABLE');
  assert.equal(body.errors[0].service, 'Divisas');
  fx.frankfurterUp = true;
  fx.erUp = true;
});

test('todos los servicios responden: contrato consolidado con SUCCESS', async () => {
  const { status, body } = await getSummary('P-101');

  assert.equal(status, 200);
  assert.deepEqual(
    {
      product_id: body.product_id,
      title: body.title,
      price_usd: body.price_usd,
      price_mxn: body.price_mxn,
      stock_status: body.stock_status,
      available_units: body.available_units,
      currency_conversion_source: body.currency_conversion_source,
      status: body.status,
    },
    {
      product_id: 'P-101',
      title: 'Laptop Gamer XYZ',
      price_usd: 1200,
      price_mxn: 24000,
      stock_status: 'IN_STOCK',
      available_units: 15,
      currency_conversion_source: 'Frankfurter External API',
      status: 'SUCCESS',
    },
  );
  assert.equal(body.errors, undefined);
});

test('la ruta alternativa /api/v1/products/:id/summary devuelve lo mismo', async () => {
  const res = await fetch(`http://localhost:${PORTS.gateway}/api/v1/products/p-101/summary`);
  const body = await res.json();
  assert.equal(body.product_id, 'P-101');
  assert.equal(body.status, 'SUCCESS');
});

test('calcula LOW_STOCK y OUT_OF_STOCK a partir de las unidades', async () => {
  assert.equal((await getSummary('P-102')).body.stock_status, 'LOW_STOCK');
  assert.equal((await getSummary('P-103')).body.stock_status, 'OUT_OF_STOCK');
});

test('Frankfurter caído: usa ExchangeRate-API como respaldo', async () => {
  fx.frankfurterUp = false;
  const { body } = await getSummary('P-101');
  fx.frankfurterUp = true;

  assert.equal(body.status, 'SUCCESS');
  assert.equal(body.price_mxn, 23400);
  assert.equal(body.currency_conversion_source, 'ExchangeRate-API (open.er-api.com)');
});

test('ambas APIs de divisas caídas: usa la última tasa conocida', async () => {
  fx.frankfurterUp = false;
  fx.erUp = false;
  const { body } = await getSummary('P-101');
  fx.frankfurterUp = true;
  fx.erUp = true;

  assert.equal(body.status, 'SUCCESS');
  assert.equal(body.price_mxn, 23400);
  assert.match(body.currency_conversion_source, /última tasa conocida/);
});

test('inventario con error 500: PARTIAL_SUCCESS con stock UNKNOWN', async () => {
  const { status, body } = await getSummary('P-105');

  assert.equal(status, 200);
  assert.equal(body.status, 'PARTIAL_SUCCESS');
  assert.equal(body.stock_status, 'UNKNOWN');
  assert.equal(body.available_units, null);
  assert.equal(body.price_mxn, 2980);
  assert.deepEqual(body.errors, [{ service: 'Inventario', message: 'respondió HTTP 500' }]);
});

test('inventario lento: se corta por timeout sin bloquear la respuesta', async () => {
  const started = Date.now();
  const { body } = await getSummary('P-104');

  assert.ok(Date.now() - started < 600, 'debe responder antes del retardo del inventario');
  assert.equal(body.status, 'PARTIAL_SUCCESS');
  assert.match(body.errors[0].message, /timeout/);
});

test('producto inexistente: 404 NOT_FOUND', async () => {
  const { status, body } = await getSummary('P-999');
  assert.equal(status, 404);
  assert.equal(body.status, 'NOT_FOUND');
});

test('catálogo caído: 503 ERROR', async () => {
  await close(servers.catalog);
  const { status, body } = await getSummary('P-101');

  assert.equal(status, 503);
  assert.equal(body.status, 'ERROR');
  assert.equal(body.errors[0].service, 'Catálogo');
});
