package org.example.achat.payment.api.dto;

import java.time.LocalDate;

public record SettlementResponse(
        Long idReglement,
        float montantPaye,
        float montantRestant,
        Boolean payee,
        LocalDate dateReglement,
        Long factureId) {
}
