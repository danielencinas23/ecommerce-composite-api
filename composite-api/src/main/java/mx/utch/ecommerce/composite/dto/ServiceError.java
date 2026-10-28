package mx.utch.ecommerce.composite.dto;

/** Detalle de un servicio que no respondió correctamente. */
public record ServiceError(String service, String message) {
}
