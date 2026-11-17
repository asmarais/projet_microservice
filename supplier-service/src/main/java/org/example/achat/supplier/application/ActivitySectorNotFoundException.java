package org.example.achat.supplier.application;

public class ActivitySectorNotFoundException extends RuntimeException {

    public ActivitySectorNotFoundException(Long id) {
        super("Secteur d'activité not found with id: " + id);
    }
}
