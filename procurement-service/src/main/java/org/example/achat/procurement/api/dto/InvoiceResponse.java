package org.example.achat.procurement.api.dto;

import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long idFacture,
        float montantRemise,
        float montantFacture,
        LocalDate dateCreationFacture,
        LocalDate dateDerniereModificationFacture,
        Boolean archivee,
        List<InvoiceLineResponse> detailsFacture) {
}
