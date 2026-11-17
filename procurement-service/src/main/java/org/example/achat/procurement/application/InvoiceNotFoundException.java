package org.example.achat.procurement.application;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(Long id) {
        super("Facture not found with id: " + id);
    }
}
