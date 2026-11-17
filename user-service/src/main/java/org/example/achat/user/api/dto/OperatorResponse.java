package org.example.achat.user.api.dto;

import java.util.Set;

public record OperatorResponse(
        Long idOperateur,
        String nom,
        String prenom,
        Set<Long> factureIds) {
}
