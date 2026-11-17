package org.example.achat.procurement.infrastructure.client;

import org.example.achat.procurement.api.dto.OperatorReferenceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface OperatorClient {

    @GetMapping("/internal/v1/operators/{operatorId}")
    OperatorReferenceResponse findOperator(@PathVariable Long operatorId);
}
