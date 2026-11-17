package org.example.achat.inventory.application;

public class StockInUseException extends RuntimeException {

    public StockInUseException(Long stockId) {
        super("Stock cannot be deleted while products are assigned: " + stockId);
    }
}
