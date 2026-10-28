// Microservicio 2 (API propia): Inventario y Almacén.
import express from 'express';
import { config } from '../../config.js';
import { runIfMain } from '../../runIfMain.js';
import { inventory } from './data.js';

export function createInventoryApp() {
  const app = express();

  app.get('/microservices/inventory/:productId', (req, res) => {
    const productId = req.params.productId.toUpperCase();
    const record = inventory.find((item) => item.productId === productId);

    if (!record) {
      return res.status(404).json({ error: 'INVENTORY_NOT_FOUND', message: `Sin registro de inventario para ${productId}` });
    }
    res.json(record);
  });

  app.get('/health', (req, res) => res.json({ service: 'inventory', status: 'UP' }));

  return app;
}

export function startInventory(port = config.inventoryPort) {
  return createInventoryApp().listen(port, () => {
    console.log(`[Inventario] http://localhost:${port}/microservices/inventory/{productId}`);
  });
}

runIfMain(import.meta.url, startInventory);
