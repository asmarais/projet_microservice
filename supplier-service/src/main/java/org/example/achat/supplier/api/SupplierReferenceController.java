package org.example.achat.supplier.api;

import org.example.achat.supplier.api.dto.SupplierReferenceResponse;
import org.example.achat.supplier.application.SupplierApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/suppliers")
public class SupplierReferenceController {

    private final SupplierApplicationService service;

    public SupplierReferenceController(SupplierApplicationService service) {
        this.service = service;
    }

    @GetMapping("/{supplierId}")
    public SupplierReferenceResponse findReference(@PathVariable Long supplierId) {
        return service.findReference(supplierId);
    }
}
