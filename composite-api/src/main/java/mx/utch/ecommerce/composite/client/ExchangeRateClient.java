package mx.utch.ecommerce.composite.client;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;

import mx.utch.ecommerce.composite.config.ServicesProperties;

/**
 * Microservicio 3 (API externa de terceros): tipo de cambio de divisas.
 * Proveedor principal: Frankfurter. Respaldo: ExchangeRate-API. Último recurso: última tasa conocida.
 */
@Component
public class ExchangeRateClient {

    public static final String SERVICE = "Divisas";
    public static final String FRANKFURTER = "Frankfurter External API";
    public static final String EXCHANGE_RATE_API = "ExchangeRate-API (open.er-api.com)";

    /** Respuesta de Frankfurter: { "amount": 1.0, "base": "USD", "date": "2026-10-28", "rates": { "MXN": 18.63 } } */
    record FrankfurterResponse(BigDecimal amount, String base, String date, Map<String, BigDecimal> rates) {
    }

    /** Respuesta de ExchangeRate-API: { "result": "success", "time_last_update_utc": "...", "rates": { "MXN": 18.64, ... } } */
    record ExchangeRateApiResponse(String result,
                                   @JsonProperty("time_last_update_utc") String timeLastUpdateUtc,
                                   Map<String, BigDecimal> rates) {
    }

    /** Tasa obtenida, el proveedor que la dio y si proviene de la caché. */
    public record ExchangeRate(BigDecimal rate, String source, String date, boolean cached) {

        ExchangeRate asCached() {
            return new ExchangeRate(rate, source, date, true);
        }
    }

    @FunctionalInterface
    private interface RateProvider {
        ExchangeRate fetch(String base, String target);
    }

    private record CachedRate(ExchangeRate rate, Instant fetchedAt) {
    }

    private final JsonHttpClient http;
    private final ServicesProperties properties;
    private final List<RateProvider> providers = List.of(this::fromFrankfurter, this::fromExchangeRateApi);
    // Última tasa obtenida con éxito por par de monedas, p. ej. "USD-MXN".
    private final Map<String, CachedRate> lastKnownRates = new ConcurrentHashMap<>();

    public ExchangeRateClient(JsonHttpClient http, ServicesProperties properties) {
        this.http = http;
        this.properties = properties;
    }

    public ExchangeRate getRate(String base, String target) {
        String pair = base + "-" + target;
        List<String> failures = new ArrayList<>();

        for (RateProvider provider : providers) {
            try {
                ExchangeRate rate = provider.fetch(base, target);
                lastKnownRates.put(pair, new CachedRate(rate, Instant.now()));
                return rate;
            } catch (ServiceException e) {
                failures.add(e.getService() + ": " + e.getMessage());
            }
        }

        // Ningún proveedor respondió: usar la última tasa conocida si no es demasiado antigua.
        CachedRate cached = lastKnownRates.get(pair);
        if (cached != null && Duration.between(cached.fetchedAt(), Instant.now()).compareTo(properties.rateCacheMaxAge()) <= 0) {
            return cached.rate().asCached();
        }

        throw new ServiceException(SERVICE, String.join(" | ", failures));
    }

    private ExchangeRate fromFrankfurter(String base, String target) {
        FrankfurterResponse response = http.get(FRANKFURTER,
                properties.frankfurterUrl() + "/latest?from={base}&to={target}", FrankfurterResponse.class, base, target);
        BigDecimal rate = response.rates() == null ? null : response.rates().get(target);
        return validate(FRANKFURTER, rate, response.date(), base, target);
    }

    private ExchangeRate fromExchangeRateApi(String base, String target) {
        ExchangeRateApiResponse response = http.get(EXCHANGE_RATE_API,
                properties.erApiUrl() + "/latest/{base}", ExchangeRateApiResponse.class, base);
        BigDecimal rate = "success".equals(response.result()) && response.rates() != null ? response.rates().get(target) : null;
        return validate(EXCHANGE_RATE_API, rate, response.timeLastUpdateUtc(), base, target);
    }

    private static ExchangeRate validate(String provider, BigDecimal rate, String date, String base, String target) {
        if (rate == null || rate.signum() <= 0) {
            throw new ServiceException(provider, "la respuesta no incluye la tasa " + base + "-" + target);
        }
        return new ExchangeRate(rate, provider, date, false);
    }
}
