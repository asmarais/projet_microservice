package org.example.achat.supplier.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivitySectorRequest(
        @NotBlank String codeSecteurActivite,
        @NotBlank String libelleSecteurActivite) {
}
