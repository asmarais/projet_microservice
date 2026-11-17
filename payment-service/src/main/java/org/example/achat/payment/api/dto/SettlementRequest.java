package org.example.achat.payment.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record SettlementRequest(
        @PositiveOrZero float montantPaye,
        @PositiveOrZero float montantRestant,
        @NotNull Boolean payee,
        @NotNull LocalDate dateReglement,
        @NotNull Long factureId) {
}
