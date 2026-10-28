package mx.utch.ecommerce.inventory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import mx.utch.ecommerce.inventory.InventoryRecord.Warehouse;

/** Datos en memoria del inventario (simulan el sistema del almacén). */
@Repository
public class InventoryRepository {

    private final Map<String, InventoryRecord> inventory = new LinkedHashMap<>();

    public InventoryRepository() {
        save(new InventoryRecord("P-101", 15, new Warehouse("CUU-01", "Chihuahua, Chih."), 2));
        save(new InventoryRecord("P-102", 3, new Warehouse("MTY-02", "Monterrey, N.L."), 4));
        save(new InventoryRecord("P-103", 0, new Warehouse("CDMX-01", "Ciudad de México"), 10));
        save(new InventoryRecord("P-104", 42, new Warehouse("CUU-01", "Chihuahua, Chih."), 2));
        save(new InventoryRecord("P-105", 8, new Warehouse("GDL-01", "Guadalajara, Jal."), 3));
    }

    private void save(InventoryRecord record) {
        inventory.put(record.productId(), record);
    }

    public Optional<InventoryRecord> findByProductId(String productId) {
        return Optional.ofNullable(inventory.get(productId));
    }
}
