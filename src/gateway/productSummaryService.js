// Lógica de orquestación de la Composite API: consulta los 3 servicios en paralelo
// y consolida sus respuestas en un único contrato JSON.
import { config } from '../config.js';
import { fetchJson } from './httpClient.js';
import { getExchangeRate } from './exchangeRateClient.js';

const LOW_STOCK_THRESHOLD = 5;

function toStockStatus(units) {
  if (units <= 0) return 'OUT_OF_STOCK';
  if (units <= LOW_STOCK_THRESHOLD) return 'LOW_STOCK';
  return 'IN_STOCK';
}

const round2 = (n) => Math.round(n * 100) / 100;

export async function buildProductSummary(productId) {
  const id = encodeURIComponent(productId.toUpperCase());

  // Las 3 peticiones salen al mismo tiempo. allSettled (a diferencia de Promise.all)
  // espera a todas aunque alguna falle, para poder responder con datos parciales.
  const [productResult, inventoryResult, rateResult] = await Promise.allSettled([
    fetchJson('Catálogo', `${config.catalogUrl}/microservices/products/${id}`),
    fetchJson('Inventario', `${config.inventoryUrl}/microservices/inventory/${id}`),
    getExchangeRate('USD', 'MXN'),
  ]);

  // El catálogo es indispensable: sin producto no hay resumen. El gateway decide el código HTTP.
  if (productResult.status === 'rejected') {
    throw productResult.reason;
  }

  const product = productResult.value;
  const inventory = inventoryResult.status === 'fulfilled' ? inventoryResult.value : null;
  const exchange = rateResult.status === 'fulfilled' ? rateResult.value : null;

  const errors = [];
  if (!inventory) errors.push({ service: 'Inventario', message: inventoryResult.reason.message });
  if (!exchange) errors.push({ service: 'Divisas', message: rateResult.reason.message });

  let conversionSource = 'UNAVAILABLE';
  if (exchange) {
    conversionSource = exchange.cached ? `${exchange.source} (última tasa conocida)` : exchange.source;
  }

  const summary = {
    product_id: product.id,
    title: product.name,
    description: product.description,
    category: product.category,
    price_usd: product.basePriceUsd,
    price_mxn: exchange ? round2(product.basePriceUsd * exchange.rate) : null,
    exchange_rate_usd_mxn: exchange?.rate ?? null,
    stock_status: inventory ? toStockStatus(inventory.quantityAvailable) : 'UNKNOWN',
    available_units: inventory?.quantityAvailable ?? null,
    warehouse: inventory ? `${inventory.warehouse.code} - ${inventory.warehouse.city}` : null,
    estimated_delivery_days: inventory?.estimatedDeliveryDays ?? null,
    currency_conversion_source: conversionSource,
    status: errors.length === 0 ? 'SUCCESS' : 'PARTIAL_SUCCESS',
  };

  if (errors.length > 0) summary.errors = errors;
  return summary;
}
