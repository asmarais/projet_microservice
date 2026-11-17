package org.example.achat.inventory.infrastructure.product.dto;

public record ProductResponse(
        Long idProduit,
        String codeProduit,
        String libelleProduit,
        float prix,
        Long stockId,
        Long categorieProduitId) {

    public ProductResponse withStockId(Long assignedStockId) {
        return new ProductResponse(
                idProduit,
                codeProduit,
                libelleProduit,
                prix,
                assignedStockId,
                categorieProduitId);
    }
}
