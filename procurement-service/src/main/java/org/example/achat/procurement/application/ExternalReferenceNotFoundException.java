package org.example.achat.procurement.application;

public class ExternalReferenceNotFoundException extends RuntimeException {

    public ExternalReferenceNotFoundException(String referenceType, Long id) {
        super(referenceType + " not found with id: " + id);
    }
}
