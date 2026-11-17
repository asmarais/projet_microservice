package org.example.achat.user.application;

public class OperatorNotFoundException extends RuntimeException {

    public OperatorNotFoundException(Long id) {
        super("Operateur not found with id: " + id);
    }
}
