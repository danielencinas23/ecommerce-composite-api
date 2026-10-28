package mx.utch.ecommerce.composite;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Pruebas de integración de la Composite API. Un servidor HTTP falso hace el papel de los
 * microservicios y de las APIs de divisas para poder provocar cada tipo de falla.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductSummaryIntegrationTest {

    private static final HttpServer fakeServices = startFakeServices();

    // Estado de los servicios falsos
    private static volatile boolean catalogUp = true;
    private static volatile boolean frankfurterUp = true;
    private static volatile boolean exchangeRateApiUp = true;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void servicesUrls(DynamicPropertyRegistry registry) {
        String base = "http://localhost:" + fakeServices.getAddress().getPort();
        registry.add("services.catalog-url", () -> base);
        registry.add("services.inventory-url", () -> base);
        registry.add("services.frankfurter-url", () -> base + "/frankfurter");
        registry.add("services.er-api-url", () -> base + "/er");
        registry.add("services.timeout", () -> "300ms");
    }

    @AfterEach
    void restoreServices() {
        catalogUp = true;
        frankfurterUp = true;
        exchangeRateApiUp = true;
    }

    @AfterAll
    static void stopFakeServices() {
        fakeServices.stop(0);
    }

    @Test
    @Order(1)
    void sinApisDeDivisasNiTasaPreviaElPrecioMxnEsNulo() throws Exception {
        frankfurterUp = false;
        exchangeRateApiUp = false;

        mockMvc.perform(get("/api/v1/product-summary/P-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIAL_SUCCESS"))
                .andExpect(jsonPath("$.price_mxn").isEmpty())
                .andExpect(jsonPath("$.currency_conversion_source").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.errors[0].service").value("Divisas"));
    }

    @Test
    @Order(2)
    void todosLosServiciosRespondenConContratoConsolidado() throws Exception {
        mockMvc.perform(get("/api/v1/product-summary/P-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value("P-101"))
                .andExpect(jsonPath("$.title").value("Laptop Gamer XYZ"))
                .andExpect(jsonPath("$.price_usd").value(1200.00))
                .andExpect(jsonPath("$.price_mxn").value(24000.00))
                .andExpect(jsonPath("$.stock_status").value("IN_STOCK"))
                .andExpect(jsonPath("$.available_units").value(15))
                .andExpect(jsonPath("$.currency_conversion_source").value("Frankfurter External API"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @Order(3)
    void laRutaAlternativaDevuelveLoMismo() throws Exception {
        mockMvc.perform(get("/api/v1/products/p-101/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value("P-101"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @Order(4)
    void calculaLowStockYOutOfStock() throws Exception {
        mockMvc.perform(get("/api/v1/product-summary/P-102")).andExpect(jsonPath("$.stock_status").value("LOW_STOCK"));
        mockMvc.perform(get("/api/v1/product-summary/P-103")).andExpect(jsonPath("$.stock_status").value("OUT_OF_STOCK"));
    }

    @Test
    @Order(5)
    void frankfurterCaidoUsaExchangeRateApiComoRespaldo() throws Exception {
        frankfurterUp = false;

        mockMvc.perform(get("/api/v1/product-summary/P-101"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.price_mxn").value(23400.00))
                .andExpect(jsonPath("$.currency_conversion_source").value("ExchangeRate-API (open.er-api.com)"));
    }

    @Test
    @Order(6)
    void ambasApisDeDivisasCaidasUsaLaUltimaTasaConocida() throws Exception {
        frankfurterUp = false;
        exchangeRateApiUp = false;

        mockMvc.perform(get("/api/v1/product-summary/P-101"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.price_mxn").value(23400.00))
                .andExpect(jsonPath("$.currency_conversion_source").value(containsString("última tasa conocida")));
    }

    @Test
    @Order(7)
    void inventarioConError500DevuelvePartialSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/product-summary/P-105"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIAL_SUCCESS"))
                .andExpect(jsonPath("$.stock_status").value("UNKNOWN"))
                .andExpect(jsonPath("$.available_units").isEmpty())
                .andExpect(jsonPath("$.price_mxn").value(2980.00))
                .andExpect(jsonPath("$.errors[0].service").value("Inventario"))
                .andExpect(jsonPath("$.errors[0].message").value("respondió HTTP 500"));
    }

    @Test
    @Order(8)
    void inventarioLentoSeCortaPorTimeout() throws Exception {
        long started = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/product-summary/P-104"))
                .andExpect(jsonPath("$.status").value("PARTIAL_SUCCESS"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("timeout")));

        org.hamcrest.MatcherAssert.assertThat(System.currentTimeMillis() - started, lessThan(1000L));
    }

    @Test
    @Order(9)
    void productoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/product-summary/P-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    @Order(10)
    void catalogoCaidoDevuelve503() throws Exception {
        catalogUp = false;

        mockMvc.perform(get("/api/v1/product-summary/P-101"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errors[0].service").value("Catálogo"));
    }

    // ---------------------------------------------------------------------------------------------
    // Servicios falsos
    // ---------------------------------------------------------------------------------------------

    private static HttpServer startFakeServices() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/microservices/products/", ProductSummaryIntegrationTest::catalog);
            server.createContext("/microservices/inventory/", ProductSummaryIntegrationTest::inventory);
            server.createContext("/frankfurter/latest", exchange -> {
                if (frankfurterUp) {
                    send(exchange, 200, "{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-10-28\",\"rates\":{\"MXN\":20.0}}");
                } else {
                    send(exchange, 503, "{\"message\":\"down\"}");
                }
            });
            server.createContext("/er/latest/USD", exchange -> {
                if (exchangeRateApiUp) {
                    send(exchange, 200, "{\"result\":\"success\",\"time_last_update_utc\":\"Wed, 28 Oct 2026\",\"rates\":{\"MXN\":19.5,\"EUR\":0.9}}");
                } else {
                    send(exchange, 503, "{\"result\":\"error\"}");
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void catalog(HttpExchange exchange) throws IOException {
        if (!catalogUp) {
            send(exchange, 503, "{\"error\":\"down\"}");
            return;
        }
        String id = lastSegment(exchange);
        String body = switch (id) {
            case "P-101" -> product(id, "Laptop Gamer XYZ", "1200.00");
            case "P-102" -> product(id, "Monitor", "329.99");
            case "P-103" -> product(id, "Teclado", "89.50");
            case "P-104" -> product(id, "Mouse", "49.99");
            case "P-105" -> product(id, "Audífonos", "149.00");
            default -> null;
        };
        if (body == null) {
            send(exchange, 404, "{\"error\":\"PRODUCT_NOT_FOUND\"}");
        } else {
            send(exchange, 200, body);
        }
    }

    private static void inventory(HttpExchange exchange) throws IOException {
        String id = lastSegment(exchange);
        switch (id) {
            case "P-101" -> send(exchange, 200, inventoryRecord(id, 15));
            case "P-102" -> send(exchange, 200, inventoryRecord(id, 3));
            case "P-103" -> send(exchange, 200, inventoryRecord(id, 0));
            case "P-104" -> {
                sleep(800);
                send(exchange, 200, inventoryRecord(id, 42));
            }
            case "P-105" -> send(exchange, 500, "{\"error\":\"WAREHOUSE_UNAVAILABLE\"}");
            default -> send(exchange, 404, "{\"error\":\"INVENTORY_NOT_FOUND\"}");
        }
    }

    private static String product(String id, String name, String price) {
        return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"description\":\"Descripción\","
                + "\"category\":\"Pruebas\",\"basePriceUsd\":" + price + "}";
    }

    private static String inventoryRecord(String id, int units) {
        return "{\"productId\":\"" + id + "\",\"quantityAvailable\":" + units
                + ",\"warehouse\":{\"code\":\"CUU-01\",\"city\":\"Chihuahua, Chih.\"},\"estimatedDeliveryDays\":2}";
    }

    private static String lastSegment(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        try {
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } catch (IOException ignored) {
            // El cliente ya cerró la conexión (p. ej. por timeout).
        }
    }
}
