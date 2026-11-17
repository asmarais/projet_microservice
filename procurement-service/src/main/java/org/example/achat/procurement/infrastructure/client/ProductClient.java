package org.example.achat.procurement.infrastructure.client;

import org.example.achat.procurement.api.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/produits/{productId}")
    ProductResponse findProduct(@PathVariable("productId") Long productId);
}
