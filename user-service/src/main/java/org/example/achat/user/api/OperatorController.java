package org.example.achat.user.api;

import jakarta.validation.Valid;
import org.example.achat.user.api.dto.OperatorRequest;
import org.example.achat.user.api.dto.OperatorResponse;
import org.example.achat.user.application.OperatorApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operateurs")
public class OperatorController {

    private final OperatorApplicationService service;

    public OperatorController(OperatorApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<OperatorResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{operateurId}")
    public OperatorResponse findById(@PathVariable Long operateurId) {
        return service.findById(operateurId);
    }

    @PostMapping
    public ResponseEntity<OperatorResponse> create(@Valid @RequestBody OperatorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{operateurId}")
    public OperatorResponse update(
            @PathVariable Long operateurId,
            @Valid @RequestBody OperatorRequest request) {
        return service.update(operateurId, request);
    }

    @DeleteMapping("/{operateurId}")
    public ResponseEntity<Void> delete(@PathVariable Long operateurId) {
        service.delete(operateurId);
        return ResponseEntity.noContent().build();
    }
}
