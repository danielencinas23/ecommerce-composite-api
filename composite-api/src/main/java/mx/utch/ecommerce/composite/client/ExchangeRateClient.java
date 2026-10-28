package mx.utch.ecommerce.composite.client;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;

import mx.utch.ecommerce.composite.config.ServicesProperties;

/** Microservicio 3 (API externa de terceros): tipo de cambio de divisas con Frankfurter. */
@Component
public class ExchangeRateClient {

    public static final String SERVICE = "Divisas";
    public static final String FRANKFURTER = "Frankfurter External API";

    /** Respuesta de Frankfurter: { "amount": 1.0, "base": "USD", "date": "2026-10-28", "rates": { "MXN": 18.63 } } */
    record FrankfurterResponse(BigDecimal amount, String base, String date, Map<String, BigDecimal> rates) {
    }

    /** Tasa obtenida, el proveedor que la dio y si proviene de la caché. */
    public record ExchangeRate(BigDecimal rate, String source, String date, boolean cached) {
    }

    private final JsonHttpClient http;
    private final ServicesProperties properties;

    public ExchangeRateClient(JsonHttpClient http, ServicesProperties properties) {
        this.http = http;
        this.properties = properties;
    }

    public ExchangeRate getRate(String base, String target) {
        try {
            FrankfurterResponse response = http.get(FRANKFURTER,
                    properties.frankfurterUrl() + "/latest?from={base}&to={target}", FrankfurterResponse.class, base, target);
            BigDecimal rate = response.rates() == null ? null : response.rates().get(target);

            if (rate == null || rate.signum() <= 0) {
                throw new ServiceException(FRANKFURTER, "la respuesta no incluye la tasa " + base + "-" + target);
            }
            return new ExchangeRate(rate, FRANKFURTER, response.date(), false);
        } catch (ServiceException e) {
            throw new ServiceException(SERVICE, e.getService() + ": " + e.getMessage(), e.getStatus(), e);
        }
    }
}
