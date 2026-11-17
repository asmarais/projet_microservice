package org.example.achat.procurement.api.dto;

import jakarta.validation.constraints.NotNull;

public record ProductReferenceRequest(@NotNull Long idProduit) {
}
