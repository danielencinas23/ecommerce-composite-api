package mx.utch.ecommerce.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devuelveElProductoPorId() throws Exception {
        mockMvc.perform(get("/microservices/products/p-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("P-101"))
                .andExpect(jsonPath("$.name").value("Laptop Gamer XYZ"))
                .andExpect(jsonPath("$.basePriceUsd").value(1200.00));
    }

    @Test
    void productoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/microservices/products/P-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }
}
