package mx.utch.ecommerce.composite.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URLs de los servicios y tiempos de espera, leídos de application.properties (prefijo "services").
 *
 * @param catalogUrl      URL base del microservicio de Catálogo
 * @param inventoryUrl    URL base del microservicio de Inventario
 * @param frankfurterUrl  URL base de la API externa Frankfurter (proveedor principal de divisas)
 * @param erApiUrl        URL base de ExchangeRate-API (proveedor de respaldo)
 * @param timeout         tiempo máximo de espera por servicio
 * @param rateCacheMaxAge antigüedad máxima de la última tasa conocida para usarla como respaldo
 */
@ConfigurationProperties(prefix = "services")
public record ServicesProperties(
        String catalogUrl,
        String inventoryUrl,
        String frankfurterUrl,
        String erApiUrl,
        Duration timeout,
        Duration rateCacheMaxAge) {
}
