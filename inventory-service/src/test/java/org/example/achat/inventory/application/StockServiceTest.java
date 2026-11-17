package org.example.achat.inventory.application;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.example.achat.inventory.api.dto.StockRequest;
import org.example.achat.inventory.domain.Stock;
import org.example.achat.inventory.domain.StockProductAssignment;
import org.example.achat.inventory.infrastructure.persistence.StockProductAssignmentRepository;
import org.example.achat.inventory.infrastructure.persistence.StockRepository;
import org.example.achat.inventory.infrastructure.product.ProductClient;
import org.example.achat.inventory.infrastructure.product.dto.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockProductAssignmentRepository assignmentRepository;

    @Mock
    private ProductClient productClient;

    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockService(stockRepository, assignmentRepository, productClient);
    }

    @Test
    void createStockPersistsExistingStockFields() {
        StockRequest request = new StockRequest("Main warehouse", 20, 5);
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> {
            Stock stock = invocation.getArgument(0);
            stock.setIdStock(3L);
            return stock;
        });

        Stock saved = stockService.create(request);

        assertThat(saved.getIdStock()).isEqualTo(3L);
        assertThat(saved.getLibelleStock()).isEqualTo("Main warehouse");
        assertThat(saved.getQte()).isEqualTo(20);
        assertThat(saved.getQteMin()).isEqualTo(5);
    }

    @Test
    void assignsAnExistingProductUsingItsScalarId() {
        Stock stock = stock(7L, "Main warehouse", 20, 5);
        ProductResponse product = new ProductResponse(11L, "P-11", "Paper", 2.5f, null, 4L);
        when(productClient.getProduct(11L)).thenReturn(product);
        when(stockRepository.findById(7L)).thenReturn(Optional.of(stock));
        when(assignmentRepository.findFirstByProductId(11L)).thenReturn(null);

        ProductResponse assigned = stockService.assignProduct(11L, 7L);

        assertThat(assigned).isEqualTo(product.withStockId(7L));
        ArgumentCaptor<StockProductAssignment> assignment =
                ArgumentCaptor.forClass(StockProductAssignment.class);
        verify(assignmentRepository).save(assignment.capture());
        assertThat(assignment.getValue().getProductId()).isEqualTo(11L);
        assertThat(assignment.getValue().getStock()).isSameAs(stock);
    }

    @Test
    void rejectsAssignmentWhenProductServiceReturnsNotFound() {
        when(productClient.getProduct(404L)).thenThrow(productNotFound(404L));

        assertThatThrownBy(() -> stockService.assignProduct(404L, 7L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found with id: 404");

        verify(stockRepository, never()).findById(7L);
        verify(assignmentRepository, never()).save(any(StockProductAssignment.class));
    }

    @Test
    void reportsProductServiceUnavailableInsteadOfSavingAssignment() {
        when(productClient.getProduct(11L)).thenThrow(
                FeignException.errorStatus("getProduct", response(503)));

        assertThatThrownBy(() -> stockService.assignProduct(11L, 7L))
                .isInstanceOf(ProductServiceUnavailableException.class);

        verify(assignmentRepository, never()).save(any(StockProductAssignment.class));
    }

    @Test
    void updatesExistingStockFields() {
        Stock existing = stock(7L, "Old label", 10, 4);
        when(stockRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(stockRepository.save(existing)).thenReturn(existing);

        Stock updated = stockService.update(7L, new StockRequest("New label", 8, 3));

        assertThat(updated.getLibelleStock()).isEqualTo("New label");
        assertThat(updated.getQte()).isEqualTo(8);
        assertThat(updated.getQteMin()).isEqualTo(3);
    }

    @Test
    void deletesStockWithoutAssignments() {
        when(stockRepository.existsById(7L)).thenReturn(true);
        when(assignmentRepository.existsByStock_IdStock(7L)).thenReturn(false);

        stockService.delete(7L);

        verify(stockRepository).deleteById(7L);
    }

    @Test
    void generatesTheExistingLowStockReport() {
        when(stockRepository.findStocksBelowMinimumQuantity())
                .thenReturn(List.of(stock(7L, "Main warehouse", 2, 5)));

        assertThat(stockService.generateLowStockReport())
                .contains("Main warehouse")
                .contains("2")
                .contains("5");
    }

    private static Stock stock(Long id, String label, int quantity, int minimum) {
        Stock stock = new Stock();
        stock.setIdStock(id);
        stock.setLibelleStock(label);
        stock.setQte(quantity);
        stock.setQteMin(minimum);
        return stock;
    }

    private static FeignException productNotFound(Long productId) {
        return FeignException.errorStatus(
                "getProduct",
                response(404, "http://product-service/produits/" + productId));
    }

    private static Response response(int status) {
        return response(status, "http://product-service/produits/11");
    }

    private static Response response(int status, String url) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                url,
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null);
        return Response.builder()
                .status(status)
                .reason(status == 404 ? "Not Found" : "Unavailable")
                .request(request)
                .headers(Map.of())
                .body(new byte[0])
                .build();
    }
}
