package org.example.achat.payment.application;

public class InvoiceNotEligibleException extends RuntimeException {

    public InvoiceNotEligibleException(String reasonCode) {
        super("Invoice is not eligible for settlement: " + reasonCode);
    }
}
