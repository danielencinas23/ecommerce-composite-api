package mx.utch.ecommerce.composite.config;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    /**
     * Cliente HTTP común para todas las llamadas salientes de la Composite API.
     * Usa el HttpClient del JDK con tiempo máximo de conexión y de lectura.
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder, ServicesProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        return builder
                .requestFactory(requestFactory)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /** Hilos virtuales (Java 21) para ejecutar las 3 peticiones en paralelo. */
    @Bean(destroyMethod = "close")
    public ExecutorService serviceCallExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
