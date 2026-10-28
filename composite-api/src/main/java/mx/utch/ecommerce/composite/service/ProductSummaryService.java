package mx.utch.ecommerce.composite.service;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import mx.utch.ecommerce.composite.client.CatalogClient;
import mx.utch.ecommerce.composite.client.CatalogClient.ProductDto;
import mx.utch.ecommerce.composite.client.ExchangeRateClient;
import mx.utch.ecommerce.composite.client.ExchangeRateClient.ExchangeRate;
import mx.utch.ecommerce.composite.client.InventoryClient;
import mx.utch.ecommerce.composite.client.InventoryClient.InventoryDto;
import mx.utch.ecommerce.composite.client.ServiceException;
import mx.utch.ecommerce.composite.dto.ProductSummary;
import mx.utch.ecommerce.composite.dto.ServiceError;
import mx.utch.ecommerce.composite.dto.StockStatus;
import mx.utch.ecommerce.composite.dto.SummaryStatus;

/**
 * Lógica de orquestación de la Composite API: consulta los 3 servicios en paralelo
 * y consolida sus respuestas en un único contrato JSON.
 */
@Service
public class ProductSummaryService {

    /** Resultado de una llamada: el valor o el error, nunca ambos (equivalente a Promise.allSettled). */
    private record Outcome<T>(T value, ServiceException error) {

        boolean failed() {
            return error != null;
        }

        static <T> Outcome<T> of(T value, Throwable error) {
            if (error == null) {
                return new Outcome<>(value, null);
            }
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            ServiceException serviceError = cause instanceof ServiceException se
                    ? se
                    : new ServiceException("Gateway", cause.getMessage(), null, cause);
            return new Outcome<>(null, serviceError);
        }
    }

    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;
    private final ExchangeRateClient exchangeRateClient;
    private final ExecutorService executor;

    public ProductSummaryService(CatalogClient catalogClient, InventoryClient inventoryClient,
                                 ExchangeRateClient exchangeRateClient, ExecutorService serviceCallExecutor) {
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.exchangeRateClient = exchangeRateClient;
        this.executor = serviceCallExecutor;
    }

    public ProductSummary buildSummary(String productId) {
        String id = productId.toUpperCase(Locale.ROOT);

        // Las 3 peticiones salen al mismo tiempo, cada una en su propio hilo virtual.
        // handle() convierte el éxito o la falla en un Outcome, así ninguna falla cancela a las demás.
        CompletableFuture<Outcome<ProductDto>> productFuture = callAsync(() -> catalogClient.getProduct(id));
        CompletableFuture<Outcome<InventoryDto>> inventoryFuture = callAsync(() -> inventoryClient.getInventory(id));
        CompletableFuture<Outcome<ExchangeRate>> rateFuture = callAsync(() -> exchangeRateClient.getRate("USD", "MXN"));

        // Espera a que terminen las tres.
        CompletableFuture.allOf(productFuture, inventoryFuture, rateFuture).join();

        Outcome<ProductDto> productResult = productFuture.join();
        Outcome<InventoryDto> inventoryResult = inventoryFuture.join();
        Outcome<ExchangeRate> rateResult = rateFuture.join();

        // El catálogo es indispensable: sin producto no hay resumen. El controlador decide el código HTTP.
        if (productResult.failed()) {
            throw productResult.error();
        }

        ProductDto product = productResult.value();
        InventoryDto inventory = inventoryResult.value();
        ExchangeRate exchange = rateResult.value();

        List<ServiceError> errors = new ArrayList<>();
        if (inventoryResult.failed()) {
            errors.add(new ServiceError(InventoryClient.SERVICE, inventoryResult.error().getMessage()));
        }
        if (rateResult.failed()) {
            errors.add(new ServiceError(ExchangeRateClient.SERVICE, rateResult.error().getMessage()));
        }

        return new ProductSummary(
                product.id(),
                product.name(),
                product.description(),
                product.category(),
                product.basePriceUsd(),
                exchange != null ? product.basePriceUsd().multiply(exchange.rate()).setScale(2, RoundingMode.HALF_UP) : null,
                exchange != null ? exchange.rate() : null,
                inventory != null ? StockStatus.fromUnits(inventory.quantityAvailable()) : StockStatus.UNKNOWN,
                inventory != null ? inventory.quantityAvailable() : null,
                inventory != null ? inventory.warehouse().code() + " - " + inventory.warehouse().city() : null,
                inventory != null ? inventory.estimatedDeliveryDays() : null,
                conversionSource(exchange),
                errors.isEmpty() ? SummaryStatus.SUCCESS : SummaryStatus.PARTIAL_SUCCESS,
                errors);
    }

    private <T> CompletableFuture<Outcome<T>> callAsync(Supplier<T> call) {
        return CompletableFuture.supplyAsync(call, executor).handle(Outcome::of);
    }

    private static String conversionSource(ExchangeRate exchange) {
        if (exchange == null) {
            return "UNAVAILABLE";
        }
        return exchange.cached() ? exchange.source() + " (última tasa conocida)" : exchange.source();
    }
}
