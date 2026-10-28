package mx.utch.ecommerce.catalog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

/** Datos en memoria del catálogo (simulan una base de datos). */
@Repository
public class ProductRepository {

    private final Map<String, Product> products = new LinkedHashMap<>();

    public ProductRepository() {
        save(new Product("P-101", "Laptop Gamer XYZ",
                "Laptop de 16\" con RTX 4070, 32 GB de RAM y SSD de 1 TB.", "Computadoras", new BigDecimal("1200.00")));
        save(new Product("P-102", "Monitor Curvo 27\" 165 Hz",
                "Panel VA QHD con 1 ms de respuesta y FreeSync Premium.", "Monitores", new BigDecimal("329.99")));
        save(new Product("P-103", "Teclado Mecánico RGB",
                "Switches rojos, formato TKL y cable USB-C desmontable.", "Periféricos", new BigDecimal("89.50")));
        save(new Product("P-104", "Mouse Inalámbrico Pro",
                "Sensor de 26K DPI, 63 g de peso y batería de 90 horas.", "Periféricos", new BigDecimal("49.99")));
        save(new Product("P-105", "Audífonos Bluetooth ANC",
                "Cancelación activa de ruido y 30 horas de autonomía.", "Audio", new BigDecimal("149.00")));
    }

    private void save(Product product) {
        products.put(product.id(), product);
    }

    public Collection<Product> findAll() {
        return products.values();
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(products.get(id));
    }
}
