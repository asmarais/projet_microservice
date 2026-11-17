package org.example.achat.procurement.api.dto;

public record InvoiceLineResponse(
        Long idDetailFacture,
        Integer qteCommandee,
        float prixTotalDetail,
        Integer pourcentageRemise,
        float montantRemise,
        ProductResponse produit) {
}
