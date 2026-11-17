package org.example.achat.supplier.api;

import jakarta.validation.Valid;
import org.example.achat.supplier.api.dto.ActivitySectorRequest;
import org.example.achat.supplier.api.dto.ActivitySectorResponse;
import org.example.achat.supplier.application.SupplierApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/secteurs-activite")
public class ActivitySectorController {

    private final SupplierApplicationService service;

    public ActivitySectorController(SupplierApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActivitySectorResponse> findAll() {
        return service.findAllSectors();
    }

    @GetMapping("/{secteurActiviteId}")
    public ActivitySectorResponse findById(@PathVariable Long secteurActiviteId) {
        return service.findSector(secteurActiviteId);
    }

    @PostMapping
    public ResponseEntity<ActivitySectorResponse> create(
            @Valid @RequestBody ActivitySectorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSector(request));
    }

    @PutMapping("/{secteurActiviteId}")
    public ActivitySectorResponse update(
            @PathVariable Long secteurActiviteId,
            @Valid @RequestBody ActivitySectorRequest request) {
        return service.updateSector(secteurActiviteId, request);
    }

    @DeleteMapping("/{secteurActiviteId}")
    public ResponseEntity<Void> delete(@PathVariable Long secteurActiviteId) {
        service.deleteSector(secteurActiviteId);
        return ResponseEntity.noContent().build();
    }
}
