package mx.utch.ecommerce.inventory;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/microservices/inventory")
public class InventoryController {

    // Fallas simuladas para demostrar la resiliencia de la Composite API:
    //  - P-104: el almacén tarda más que el timeout del gateway.
    //  - P-105: el almacén responde con un error interno.
    private static final String SLOW_PRODUCT = "P-104";
    private static final String FAILING_PRODUCT = "P-105";

    private final InventoryRepository repository;
    private final long simulatedDelayMs;

    public InventoryController(InventoryRepository repository,
                               @Value("${inventory.simulated-delay-ms:5000}") long simulatedDelayMs) {
        this.repository = repository;
        this.simulatedDelayMs = simulatedDelayMs;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> findByProductId(@PathVariable String productId) throws InterruptedException {
        String id = productId.toUpperCase(Locale.ROOT);

        if (FAILING_PRODUCT.equals(id)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "WAREHOUSE_UNAVAILABLE",
                    "message", "El sistema del almacén no responde"));
        }
        if (SLOW_PRODUCT.equals(id)) {
            Thread.sleep(simulatedDelayMs);
        }

        return repository.findByProductId(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "INVENTORY_NOT_FOUND",
                        "message", "Sin registro de inventario para " + id)));
    }
}
