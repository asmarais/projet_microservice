package org.example.achat.payment.application;

public class InvoiceServiceUnavailableException extends RuntimeException {

    public InvoiceServiceUnavailableException(Throwable cause) {
        super("Procurement service is unavailable", cause);
    }
}
