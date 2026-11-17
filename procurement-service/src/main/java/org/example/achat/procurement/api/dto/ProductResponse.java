package org.example.achat.procurement.api.dto;

public record ProductResponse(
        Long idProduit,
        String codeProduit,
        String libelleProduit,
        Float prix,
        Long stockId,
        Long categorieProduitId) {
}
