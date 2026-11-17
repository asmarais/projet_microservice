package org.example.achat.supplier.api.dto;

import java.time.LocalDate;

public record SupplierDetailRequest(
        String email,
        LocalDate dateDebutCollaboration,
        String adresse,
        String matricule) {
}
