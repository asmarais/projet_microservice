package org.example.achat.product.api.dto;

public record ProductCategoryResponse(
        Long idCategorieProduit,
        String codeCategorie,
        String libelleCategorie) {
}
