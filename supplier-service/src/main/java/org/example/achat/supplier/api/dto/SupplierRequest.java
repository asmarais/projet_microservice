package org.example.achat.supplier.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.achat.supplier.domain.SupplierCategory;

public record SupplierRequest(
        @NotBlank String code,
        @NotBlank String libelle,
        @NotNull SupplierCategory categorieFournisseur,
        SupplierDetailRequest detailFournisseur) {
}
