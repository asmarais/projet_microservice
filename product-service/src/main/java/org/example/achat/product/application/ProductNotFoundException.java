package org.example.achat.product.application;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Produit not found with id: " + productId);
    }
}
