package org.example.achat.inventory.api.dto;

public record StockResponse(
        Long idStock,
        String libelleStock,
        Integer qte,
        Integer qteMin) {
}
