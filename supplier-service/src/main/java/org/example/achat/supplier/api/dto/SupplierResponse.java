package org.example.achat.supplier.api.dto;

import org.example.achat.supplier.domain.SupplierCategory;

import java.util.Set;

public record SupplierResponse(
        Long idFournisseur,
        String code,
        String libelle,
        SupplierCategory categorieFournisseur,
        Long detailFournisseurId,
        Set<Long> secteurActiviteIds) {
}
