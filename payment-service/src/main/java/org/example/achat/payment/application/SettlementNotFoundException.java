package org.example.achat.payment.application;

public class SettlementNotFoundException extends RuntimeException {

    public SettlementNotFoundException(Long id) {
        super("Reglement not found with id: " + id);
    }
}
