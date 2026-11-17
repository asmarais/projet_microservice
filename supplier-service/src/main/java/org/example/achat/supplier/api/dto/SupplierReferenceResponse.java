package org.example.achat.supplier.api.dto;

public record SupplierReferenceResponse(
        Long supplierId,
        String code,
        String label) {
}
