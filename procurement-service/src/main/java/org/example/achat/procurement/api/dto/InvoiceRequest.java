package org.example.achat.procurement.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
        float montantRemise,
        float montantFacture,
        LocalDate dateCreationFacture,
        LocalDate dateDerniereModificationFacture,
        Boolean archivee,
        Long fournisseurId,
        List<@NotNull @Valid InvoiceLineRequest> detailsFacture) {
}
