package org.example.achat.inventory.api;

import jakarta.validation.Valid;
import org.example.achat.inventory.api.dto.StockRequest;
import org.example.achat.inventory.api.dto.StockResponse;
import org.example.achat.inventory.application.StockService;
import org.example.achat.inventory.domain.Stock;
import org.example.achat.inventory.infrastructure.product.dto.ProductResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/stocks")
    public List<StockResponse> getStocks() {
        return stockService.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/stocks/{stockId}")
    public StockResponse getStock(@PathVariable Long stockId) {
        return toResponse(stockService.findById(stockId));
    }

    @PostMapping("/stocks")
    public ResponseEntity<StockResponse> createStock(@Valid @RequestBody StockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(stockService.create(request)));
    }

    @PutMapping("/stocks/{stockId}")
    public StockResponse updateStock(
            @PathVariable Long stockId,
            @Valid @RequestBody StockRequest request) {
        return toResponse(stockService.update(stockId, request));
    }

    @DeleteMapping("/stocks/{stockId}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long stockId) {
        stockService.delete(stockId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/produits/{productId}/stock/{stockId}")
    public ProductResponse assignProductToStock(
            @PathVariable Long productId,
            @PathVariable Long stockId) {
        return stockService.assignProduct(productId, stockId);
    }

    private StockResponse toResponse(Stock stock) {
        return new StockResponse(
                stock.getIdStock(),
                stock.getLibelleStock(),
                stock.getQte(),
                stock.getQteMin());
    }
}
