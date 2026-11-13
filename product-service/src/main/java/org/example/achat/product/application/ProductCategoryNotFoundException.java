package org.example.achat.product.application;

public class ProductCategoryNotFoundException extends RuntimeException {

    public ProductCategoryNotFoundException(Long categoryId) {
        super("Categorie produit not found with id: " + categoryId);
    }
}
