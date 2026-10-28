package mx.utch.ecommerce.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "inventory.simulated-delay-ms=10")
@AutoConfigureMockMvc
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devuelveLasExistenciasDelProducto() throws Exception {
        mockMvc.perform(get("/microservices/inventory/P-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(15))
                .andExpect(jsonPath("$.warehouse.code").value("CUU-01"))
                .andExpect(jsonPath("$.estimatedDeliveryDays").value(2));
    }

    @Test
    void simulaErrorInternoParaP105() throws Exception {
        mockMvc.perform(get("/microservices/inventory/P-105"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("WAREHOUSE_UNAVAILABLE"));
    }

    @Test
    void productoSinRegistroDevuelve404() throws Exception {
        mockMvc.perform(get("/microservices/inventory/P-999"))
                .andExpect(status().isNotFound());
    }
}
