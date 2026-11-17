package org.example.achat.payment.api.dto;

public record InvoiceEligibilityResponse(Long invoiceId, boolean eligible, String reasonCode) {
}
