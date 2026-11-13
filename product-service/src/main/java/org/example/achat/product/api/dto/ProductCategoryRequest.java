package org.example.achat.product.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductCategoryRequest(
        @NotBlank String codeCategorie,
        @NotBlank String libelleCategorie) {
}
