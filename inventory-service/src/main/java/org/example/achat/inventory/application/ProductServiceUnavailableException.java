package org.example.achat.inventory.application;

public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(Throwable cause) {
        super("Product Service is unavailable", cause);
    }
}
