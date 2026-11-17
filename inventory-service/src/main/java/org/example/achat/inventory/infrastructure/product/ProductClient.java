package org.example.achat.inventory.infrastructure.product;

import org.example.achat.inventory.infrastructure.product.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", path = "/produits")
public interface ProductClient {

    @GetMapping("/{productId}")
    ProductResponse getProduct(@PathVariable Long productId);
}
