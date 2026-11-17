package org.example.achat.procurement.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record InvoiceLineRequest(
        @NotNull Integer qteCommandee,
        @NotNull Integer pourcentageRemise,
        @NotNull @Valid ProductReferenceRequest produit) {
}
