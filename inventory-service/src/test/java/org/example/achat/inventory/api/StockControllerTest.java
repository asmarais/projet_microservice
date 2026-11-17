package org.example.achat.inventory.api;

import org.example.achat.inventory.api.dto.StockRequest;
import org.example.achat.inventory.application.ProductNotFoundException;
import org.example.achat.inventory.application.ProductServiceUnavailableException;
import org.example.achat.inventory.application.StockService;
import org.example.achat.inventory.domain.Stock;
import org.example.achat.inventory.infrastructure.product.dto.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class StockControllerTest {

    private final StockService stockService = mock(StockService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new StockController(stockService))
                .setControllerAdvice(new StockExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listsAndRetrievesStockUsingLegacyResponseFields() throws Exception {
        when(stockService.findAll()).thenReturn(List.of(stock(3L, "Main warehouse", 20, 5)));
        when(stockService.findById(3L)).thenReturn(stock(3L, "Main warehouse", 20, 5));

        mockMvc.perform(get("/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idStock").value(3))
                .andExpect(jsonPath("$[0].qte").value(20));

        mockMvc.perform(get("/stocks/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelleStock").value("Main warehouse"));
    }

    @Test
    void createsStockAndRejectsInvalidQuantities() throws Exception {
        when(stockService.create(any(StockRequest.class)))
                .thenReturn(stock(3L, "Main warehouse", 20, 5));

        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"libelleStock":"Main warehouse","qte":20,"qteMin":5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idStock").value(3));

        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"libelleStock":"Main warehouse","qte":-1,"qteMin":5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesAndDeletesStock() throws Exception {
        when(stockService.update(org.mockito.ArgumentMatchers.eq(3L), any(StockRequest.class)))
                .thenReturn(stock(3L, "Updated", 8, 3));

        mockMvc.perform(put("/stocks/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"libelleStock":"Updated","qte":8,"qteMin":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelleStock").value("Updated"));

        mockMvc.perform(delete("/stocks/3")).andExpect(status().isNoContent());
        verify(stockService).delete(3L);
    }

    @Test
    void assignsAndReturnsProductInformation() throws Exception {
        when(stockService.assignProduct(11L, 3L))
                .thenReturn(new ProductResponse(11L, "P-11", "Paper", 2.5f, 3L, 4L));

        mockMvc.perform(put("/produits/11/stock/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduit").value(11))
                .andExpect(jsonPath("$.libelleProduit").value("Paper"))
                .andExpect(jsonPath("$.stockId").value(3));
    }

    @Test
    void mapsMissingProductAndUnavailableProductService() throws Exception {
        when(stockService.assignProduct(404L, 3L))
                .thenThrow(new ProductNotFoundException(404L));
        when(stockService.assignProduct(11L, 3L))
                .thenThrow(new ProductServiceUnavailableException(new RuntimeException()));

        mockMvc.perform(put("/produits/404/stock/3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product not found with id: 404"));

        mockMvc.perform(put("/produits/11/stock/3"))
                .andExpect(status().isServiceUnavailable());
    }

    private static Stock stock(Long id, String label, int quantity, int minimum) {
        Stock stock = new Stock();
        stock.setIdStock(id);
        stock.setLibelleStock(label);
        stock.setQte(quantity);
        stock.setQteMin(minimum);
        return stock;
    }
}
