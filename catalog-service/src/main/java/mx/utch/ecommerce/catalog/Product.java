package mx.utch.ecommerce.catalog;

import java.math.BigDecimal;

/** Información básica de un producto con su precio base en USD. */
public record Product(String id, String name, String description, String category, BigDecimal basePriceUsd) {
}
