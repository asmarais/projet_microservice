package org.example.achat.supplier.api;

import jakarta.validation.Valid;
import org.example.achat.supplier.api.dto.SupplierRequest;
import org.example.achat.supplier.api.dto.SupplierResponse;
import org.example.achat.supplier.application.SupplierApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fournisseurs")
public class SupplierController {

    private final SupplierApplicationService service;

    public SupplierController(SupplierApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupplierResponse> findAll() {
        return service.findAllSuppliers();
    }

    @GetMapping("/{fournisseurId}")
    public SupplierResponse findById(@PathVariable Long fournisseurId) {
        return service.findSupplier(fournisseurId);
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSupplier(request));
    }

    @PutMapping("/{fournisseurId}")
    public SupplierResponse update(
            @PathVariable Long fournisseurId,
            @Valid @RequestBody SupplierRequest request) {
        return service.updateSupplier(fournisseurId, request);
    }

    @DeleteMapping("/{fournisseurId}")
    public ResponseEntity<Void> delete(@PathVariable Long fournisseurId) {
        service.deleteSupplier(fournisseurId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{fournisseurId}/secteurs-activite/{secteurActiviteId}")
    public SupplierResponse assignSector(
            @PathVariable Long fournisseurId,
            @PathVariable Long secteurActiviteId) {
        return service.assignSector(fournisseurId, secteurActiviteId);
    }
}
