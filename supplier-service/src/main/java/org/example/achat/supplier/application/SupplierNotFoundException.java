package org.example.achat.supplier.application;

public class SupplierNotFoundException extends RuntimeException {

    public SupplierNotFoundException(Long id) {
        super("Fournisseur not found with id: " + id);
    }
}
