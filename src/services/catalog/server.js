// Microservicio 1 (API propia): Catálogo de Productos.
import express from 'express';
import { config } from '../../config.js';
import { runIfMain } from '../../runIfMain.js';
import { products } from './data.js';

export function createCatalogApp() {
  const app = express();

  app.get('/microservices/products', (req, res) => {
    res.json(products);
  });

  app.get('/microservices/products/:id', (req, res) => {
    const product = products.find((p) => p.id === req.params.id.toUpperCase());

    if (!product) {
      return res.status(404).json({ error: 'PRODUCT_NOT_FOUND', message: `No existe el producto ${req.params.id}` });
    }
    res.json(product);
  });

  app.get('/health', (req, res) => res.json({ service: 'catalog', status: 'UP' }));

  return app;
}

export function startCatalog(port = config.catalogPort) {
  return createCatalogApp().listen(port, () => {
    console.log(`[Catálogo]   http://localhost:${port}/microservices/products/{id}`);
  });
}

runIfMain(import.meta.url, startCatalog);
