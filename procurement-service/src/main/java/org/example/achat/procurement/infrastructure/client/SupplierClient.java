package org.example.achat.procurement.infrastructure.client;

import org.example.achat.procurement.api.dto.SupplierReferenceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "supplier-service")
public interface SupplierClient {

    @GetMapping("/internal/v1/suppliers/{supplierId}")
    SupplierReferenceResponse findSupplier(@PathVariable Long supplierId);
}
