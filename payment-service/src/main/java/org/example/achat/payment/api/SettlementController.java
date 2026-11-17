package org.example.achat.payment.api;

import jakarta.validation.Valid;
import org.example.achat.payment.api.dto.SettlementRequest;
import org.example.achat.payment.api.dto.SettlementResponse;
import org.example.achat.payment.application.SettlementApplicationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reglements")
public class SettlementController {

    private final SettlementApplicationService service;

    public SettlementController(SettlementApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<SettlementResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{reglementId}")
    public SettlementResponse findById(@PathVariable Long reglementId) {
        return service.findById(reglementId);
    }

    @PostMapping
    public ResponseEntity<SettlementResponse> create(@Valid @RequestBody SettlementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{reglementId}")
    public SettlementResponse update(
            @PathVariable Long reglementId,
            @Valid @RequestBody SettlementRequest request) {
        return service.update(reglementId, request);
    }

    @DeleteMapping("/{reglementId}")
    public ResponseEntity<Void> delete(@PathVariable Long reglementId) {
        service.delete(reglementId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/facture/{factureId}")
    public List<SettlementResponse> findByInvoice(@PathVariable Long factureId) {
        return service.findByInvoice(factureId);
    }

    @GetMapping("/revenue")
    public float revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return service.getRevenue(startDate, endDate);
    }
}
