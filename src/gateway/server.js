// Composite API (API Gateway / Aggregator): punto de entrada único para el cliente web.
import { fileURLToPath } from 'node:url';
import express from 'express';
import { config } from '../config.js';
import { runIfMain } from '../runIfMain.js';
import { buildProductSummary } from './productSummaryService.js';

const publicDir = fileURLToPath(new URL('../../public', import.meta.url));

export function createGatewayApp() {
  const app = express();

  app.use(express.static(publicDir));

  // Se exponen las dos rutas que menciona el planteamiento; ambas hacen lo mismo.
  app.get(['/api/v1/product-summary/:id', '/api/v1/products/:id/summary'], async (req, res) => {
    const started = performance.now();

    try {
      const summary = await buildProductSummary(req.params.id);
      res.set('X-Response-Time', `${Math.round(performance.now() - started)}ms`);
      res.json(summary);
    } catch (err) {
      res.set('X-Response-Time', `${Math.round(performance.now() - started)}ms`);

      // El catálogo respondió 404: el producto no existe.
      if (err.service === 'Catálogo' && err.status === 404) {
        return res.status(404).json({
          product_id: req.params.id,
          status: 'NOT_FOUND',
          message: `El producto ${req.params.id} no existe en el catálogo`,
        });
      }

      // El catálogo no respondió o falló: no se puede construir el resumen.
      console.error(`[Gateway] Error al construir el resumen de ${req.params.id}:`, err.message);
      res.status(503).json({
        product_id: req.params.id,
        status: 'ERROR',
        message: 'El servicio de catálogo no está disponible. Intente más tarde.',
        errors: [{ service: err.service ?? 'Gateway', message: err.message }],
      });
    }
  });

  app.get('/health', (req, res) => res.json({ service: 'gateway', status: 'UP' }));

  return app;
}

export function startGateway(port = config.gatewayPort) {
  return createGatewayApp().listen(port, () => {
    console.log(`[Gateway]    http://localhost:${port}/api/v1/product-summary/{id}`);
    console.log(`[Dashboard]  http://localhost:${port}`);
  });
}

runIfMain(import.meta.url, startGateway);
