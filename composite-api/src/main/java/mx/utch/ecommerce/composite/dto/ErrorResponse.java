package mx.utch.ecommerce.composite.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** Respuesta cuando no se puede construir el resumen (producto inexistente o Catálogo caído). */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ErrorResponse(
        String productId,
        SummaryStatus status,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ServiceError> errors) {
}
