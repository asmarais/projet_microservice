package org.example.achat.product.api.dto;

public record ProductResponse(
        Long idProduit,
        String codeProduit,
        String libelleProduit,
        float prix,
        Long stockId,
        Long categorieProduitId) {
}
