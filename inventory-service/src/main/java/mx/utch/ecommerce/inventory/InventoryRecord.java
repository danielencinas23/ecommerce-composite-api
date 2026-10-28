package mx.utch.ecommerce.inventory;

/**
 * Existencias de un producto en el almacén.
 * Usa nombres de campos distintos al contrato final para que la Composite API los mapee.
 */
public record InventoryRecord(String productId, int quantityAvailable, Warehouse warehouse, int estimatedDeliveryDays) {

    public record Warehouse(String code, String city) {
    }
}
