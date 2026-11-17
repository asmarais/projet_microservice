package org.example.achat.user.api.dto;

import jakarta.validation.constraints.NotBlank;

public record OperatorRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank String password) {
}
