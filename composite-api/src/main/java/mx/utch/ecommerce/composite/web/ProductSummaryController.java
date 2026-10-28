package mx.utch.ecommerce.composite.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import mx.utch.ecommerce.composite.client.CatalogClient;
import mx.utch.ecommerce.composite.client.ServiceException;
import mx.utch.ecommerce.composite.dto.ErrorResponse;
import mx.utch.ecommerce.composite.dto.ProductSummary;
import mx.utch.ecommerce.composite.dto.ServiceError;
import mx.utch.ecommerce.composite.dto.SummaryStatus;
import mx.utch.ecommerce.composite.service.ProductSummaryService;

@RestController
public class ProductSummaryController {

    private static final Logger log = LoggerFactory.getLogger(ProductSummaryController.class);

    private final ProductSummaryService summaryService;

    public ProductSummaryController(ProductSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    // Se exponen las dos rutas que menciona el planteamiento; ambas hacen lo mismo.
    @GetMapping({"/api/v1/product-summary/{id}", "/api/v1/products/{id}/summary"})
    public ResponseEntity<?> getSummary(@PathVariable String id) {
        long started = System.nanoTime();

        try {
            ProductSummary summary = summaryService.buildSummary(id);
            return ResponseEntity.ok()
                    .header("X-Response-Time", elapsedMs(started))
                    .body(summary);
        } catch (ServiceException e) {
            // El catálogo respondió 404: el producto no existe.
            if (CatalogClient.SERVICE.equals(e.getService()) && Integer.valueOf(404).equals(e.getStatus())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Response-Time", elapsedMs(started))
                        .body(new ErrorResponse(id, SummaryStatus.NOT_FOUND,
                                "El producto " + id + " no existe en el catálogo", List.of()));
            }

            // El catálogo no respondió o falló: no se puede construir el resumen.
            log.error("Error al construir el resumen de {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header("X-Response-Time", elapsedMs(started))
                    .body(new ErrorResponse(id, SummaryStatus.ERROR,
                            "El servicio de catálogo no está disponible. Intente más tarde.",
                            List.of(new ServiceError(e.getService(), e.getMessage()))));
        }
    }

    private static String elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000 + "ms";
    }
}
