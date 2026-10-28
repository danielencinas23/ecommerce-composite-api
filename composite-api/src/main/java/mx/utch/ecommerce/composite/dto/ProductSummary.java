package mx.utch.ecommerce.composite.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** Contrato JSON consolidado que la Composite API entrega al cliente (campos en snake_case). */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProductSummary(
        String productId,
        String title,
        String description,
        String category,
        BigDecimal priceUsd,
        BigDecimal priceMxn,
        BigDecimal exchangeRateUsdMxn,
        StockStatus stockStatus,
        Integer availableUnits,
        String warehouse,
        Integer estimatedDeliveryDays,
        String currencyConversionSource,
        SummaryStatus status,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ServiceError> errors) {
}
