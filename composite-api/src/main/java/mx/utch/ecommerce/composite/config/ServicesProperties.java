package mx.utch.ecommerce.composite.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs de los servicios y tiempos de espera, leídos de application.properties (prefijo "services").
 *
 * @param catalogUrl     URL base del microservicio de Catálogo
 * @param inventoryUrl   URL base del microservicio de Inventario
 * @param frankfurterUrl URL base de la API externa Frankfurter
 * @param timeout        tiempo máximo de espera por servicio
 */
@ConfigurationProperties(prefix = "services")
public record ServicesProperties(
        String catalogUrl,
        String inventoryUrl,
        String frankfurterUrl,
        Duration timeout) {
}
