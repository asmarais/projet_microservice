package org.example.achat.payment.infrastructure.client;

import org.example.achat.payment.api.dto.InvoiceEligibilityResponse;
import org.example.achat.payment.api.dto.InvoiceTotalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "procurement-service")
public interface InvoiceClient {

    @GetMapping("/internal/v1/invoices/{invoiceId}/payment-eligibility")
    InvoiceEligibilityResponse getPaymentEligibility(@PathVariable("invoiceId") Long invoiceId);

    @GetMapping("/internal/v1/invoices/total")
    InvoiceTotalResponse getInvoiceTotal(
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate);
}
