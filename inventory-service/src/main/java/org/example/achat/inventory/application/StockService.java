package org.example.achat.inventory.application;

import feign.FeignException;
import org.example.achat.inventory.api.dto.StockRequest;
import org.example.achat.inventory.domain.Stock;
import org.example.achat.inventory.domain.StockProductAssignment;
import org.example.achat.inventory.infrastructure.persistence.StockProductAssignmentRepository;
import org.example.achat.inventory.infrastructure.persistence.StockRepository;
import org.example.achat.inventory.infrastructure.product.ProductClient;
import org.example.achat.inventory.infrastructure.product.dto.ProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockProductAssignmentRepository assignmentRepository;
    private final ProductClient productClient;

    public StockService(
            StockRepository stockRepository,
            StockProductAssignmentRepository assignmentRepository,
            ProductClient productClient) {
        this.stockRepository = stockRepository;
        this.assignmentRepository = assignmentRepository;
        this.productClient = productClient;
    }

    @Transactional(readOnly = true)
    public List<Stock> findAll() {
        return stockRepository.findAll();
    }

    @Transactional(readOnly = true)
    public String generateLowStockReport() {
        StringBuilder report = new StringBuilder();
        for (Stock stock : stockRepository.findStocksBelowMinimumQuantity()) {
            report.append(": le stock ")
                    .append(stock.getLibelleStock())
                    .append(" a une quantité de ")
                    .append(stock.getQte())
                    .append(" inférieur à la quantité minimale a ne pas dépasser de ")
                    .append(stock.getQteMin())
                    .append(System.lineSeparator());
        }
        return report.toString();
    }

    @Transactional(readOnly = true)
    public Stock findById(Long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new StockNotFoundException(stockId));
    }

    @Transactional
    public Stock create(StockRequest request) {
        Stock stock = new Stock();
        apply(stock, request);
        return stockRepository.save(stock);
    }

    @Transactional
    public Stock update(Long stockId, StockRequest request) {
        Stock stock = findById(stockId);
        apply(stock, request);
        return stockRepository.save(stock);
    }

    @Transactional
    public void delete(Long stockId) {
        if (!stockRepository.existsById(stockId)) {
            throw new StockNotFoundException(stockId);
        }
        if (assignmentRepository.existsByStock_IdStock(stockId)) {
            throw new StockInUseException(stockId);
        }
        stockRepository.deleteById(stockId);
    }

    @Transactional
    public ProductResponse assignProduct(Long productId, Long stockId) {
        ProductResponse product = findProduct(productId);
        Stock stock = findById(stockId);

        StockProductAssignment assignment = assignmentRepository.findFirstByProductId(productId);
        if (assignment == null) {
            assignment = new StockProductAssignment();
            assignment.setProductId(productId);
        }
        assignment.setStock(stock);
        assignmentRepository.save(assignment);

        return product.withStockId(stockId);
    }

    private ProductResponse findProduct(Long productId) {
        try {
            ProductResponse product = productClient.getProduct(productId);
            if (product == null) {
                throw new ProductServiceUnavailableException(
                        new IllegalStateException("Product Service returned an empty response"));
            }
            return product;
        } catch (FeignException.NotFound exception) {
            throw new ProductNotFoundException(productId);
        } catch (FeignException exception) {
            throw new ProductServiceUnavailableException(exception);
        }
    }

    private void apply(Stock stock, StockRequest request) {
        stock.setLibelleStock(request.libelleStock());
        stock.setQte(request.qte());
        stock.setQteMin(request.qteMin());
    }
}
