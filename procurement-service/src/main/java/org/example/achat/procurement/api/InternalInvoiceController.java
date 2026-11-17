package org.example.achat.procurement.api;

import org.example.achat.procurement.api.dto.InvoicePaymentEligibilityResponse;
import org.example.achat.procurement.api.dto.InvoiceTotalResponse;
import org.example.achat.procurement.application.InvoiceQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/internal/v1/invoices")
public class InternalInvoiceController {

    private final InvoiceQueryService queries;

    public InternalInvoiceController(InvoiceQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/{invoiceId}/payment-eligibility")
    public InvoicePaymentEligibilityResponse paymentEligibility(@PathVariable Long invoiceId) {
        return queries.paymentEligibility(invoiceId);
    }

    @GetMapping("/total")
    public InvoiceTotalResponse total(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return queries.totalForPaymentReporting(startDate, endDate);
    }
}
