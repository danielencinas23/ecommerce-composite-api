package mx.utch.ecommerce.composite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Composite API (API Gateway / Aggregator): punto de entrada único para el cliente web. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CompositeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompositeApiApplication.class, args);
    }
}
