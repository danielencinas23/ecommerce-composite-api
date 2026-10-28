// Datos en memoria del Microservicio de Inventario (simulan el sistema del almacén).
// Usa nombres de campos distintos al contrato final para que la Composite API los mapee.
export const inventory = [
  {
    productId: 'P-101',
    quantityAvailable: 15,
    warehouse: { code: 'CUU-01', city: 'Chihuahua, Chih.' },
    estimatedDeliveryDays: 2,
  },
  {
    productId: 'P-102',
    quantityAvailable: 3,
    warehouse: { code: 'MTY-02', city: 'Monterrey, N.L.' },
    estimatedDeliveryDays: 4,
  },
  {
    productId: 'P-103',
    quantityAvailable: 0,
    warehouse: { code: 'CDMX-01', city: 'Ciudad de México' },
    estimatedDeliveryDays: 10,
  },
  {
    productId: 'P-104',
    quantityAvailable: 42,
    warehouse: { code: 'CUU-01', city: 'Chihuahua, Chih.' },
    estimatedDeliveryDays: 2,
  },
  {
    productId: 'P-105',
    quantityAvailable: 8,
    warehouse: { code: 'GDL-01', city: 'Guadalajara, Jal.' },
    estimatedDeliveryDays: 3,
  },
];

