package mx.utch.ecommerce.composite.client;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import mx.utch.ecommerce.composite.config.ServicesProperties;

/** Cliente del Microservicio 1: Catálogo de Productos. */
@Component
public class CatalogClient {

    public static final String SERVICE = "Catálogo";

    /** Respuesta del catálogo: { id, name, description, category, basePriceUsd }. */
    public record ProductDto(String id, String name, String description, String category, BigDecimal basePriceUsd) {
    }

    private final JsonHttpClient http;
    private final ServicesProperties properties;

    public CatalogClient(JsonHttpClient http, ServicesProperties properties) {
        this.http = http;
        this.properties = properties;
    }

    public ProductDto getProduct(String id) {
        return http.get(SERVICE, properties.catalogUrl() + "/microservices/products/{id}", ProductDto.class, id);
    }
}
