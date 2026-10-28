// Arranca los 3 servidores (cada uno en su propio puerto) con un solo comando: npm start
import { startCatalog } from './services/catalog/server.js';
import { startInventory } from './services/inventory/server.js';
import { startGateway } from './gateway/server.js';

startCatalog();
startInventory();
startGateway();
