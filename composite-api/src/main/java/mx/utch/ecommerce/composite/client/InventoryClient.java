package mx.utch.ecommerce.composite.client;

import org.springframework.stereotype.Component;

import mx.utch.ecommerce.composite.config.ServicesProperties;

/** Cliente del Microservicio 2: Inventario y Almacén. */
@Component
public class InventoryClient {

    public static final String SERVICE = "Inventario";

    /** Respuesta del inventario: { productId, quantityAvailable, warehouse: { code, city }, estimatedDeliveryDays }. */
    public record InventoryDto(String productId, int quantityAvailable, Warehouse warehouse, int estimatedDeliveryDays) {

        public record Warehouse(String code, String city) {
        }
    }

    private final JsonHttpClient http;
    private final ServicesProperties properties;

    public InventoryClient(JsonHttpClient http, ServicesProperties properties) {
        this.http = http;
        this.properties = properties;
    }

    public InventoryDto getInventory(String productId) {
        return http.get(SERVICE, properties.inventoryUrl() + "/microservices/inventory/{id}", InventoryDto.class, productId);
    }
}
