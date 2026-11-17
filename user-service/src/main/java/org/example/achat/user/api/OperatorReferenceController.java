package org.example.achat.user.api;

import org.example.achat.user.api.dto.OperatorReferenceResponse;
import org.example.achat.user.application.OperatorApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/operators")
public class OperatorReferenceController {

    private final OperatorApplicationService service;

    public OperatorReferenceController(OperatorApplicationService service) {
        this.service = service;
    }

    @GetMapping("/{operatorId}")
    public OperatorReferenceResponse findReference(@PathVariable Long operatorId) {
        return service.findReference(operatorId);
    }
}
