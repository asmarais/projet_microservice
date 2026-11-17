package org.example.achat.inventory.application;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(Long stockId) {
        super("Stock not found with id: " + stockId);
    }
}
