package org.example.achat.procurement.api.dto;

public record InvoicePaymentEligibilityResponse(Long invoiceId, boolean eligible, String reasonCode) {
}
