package org.example.achat.procurement.application;

public class DependencyUnavailableException extends RuntimeException {

    public DependencyUnavailableException(String service, Throwable cause) {
        super(service + " service is unavailable", cause);
    }
}
