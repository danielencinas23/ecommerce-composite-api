// Microservicio 2 (API propia): Inventario y Almacén.
import { setTimeout as sleep } from 'node:timers/promises';
import express from 'express';
import { config } from '../../config.js';
import { runIfMain } from '../../runIfMain.js';
import { inventory, simulatedFailures } from './data.js';

export function createInventoryApp() {
  const app = express();

  app.get('/microservices/inventory/:productId', async (req, res) => {
    const productId = req.params.productId.toUpperCase();
    const failure = simulatedFailures[productId];

    if (failure === 'ERROR') {
      return res.status(500).json({ error: 'WAREHOUSE_UNAVAILABLE', message: 'El sistema del almacén no responde' });
    }
    if (failure === 'SLOW') {
      await sleep(config.simulatedDelayMs);
    }

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
