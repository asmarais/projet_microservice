package org.example.achat.procurement.api;

import jakarta.validation.Valid;
import org.example.achat.procurement.api.dto.InvoiceRequest;
import org.example.achat.procurement.api.dto.InvoiceResponse;
import org.example.achat.procurement.application.InvoiceCommandService;
import org.example.achat.procurement.application.InvoiceQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/facture")
public class InvoiceController {

    private final InvoiceCommandService commands;
    private final InvoiceQueryService queries;

    public InvoiceController(InvoiceCommandService commands, InvoiceQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @GetMapping("/retrieve-all-factures")
    public List<InvoiceResponse> findAll() {
        return queries.findAll();
    }

    @GetMapping("/retrieve-facture/{facture-id}")
    public ResponseEntity<InvoiceResponse> findById(@PathVariable("facture-id") Long invoiceId) {
        return ResponseEntity.ok(queries.findById(invoiceId));
    }

    @PostMapping("/add-facture")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(commands.create(request));
    }

    @PutMapping("/cancel-facture/{facture-id}")
    public ResponseEntity<Void> cancel(@PathVariable("facture-id") Long invoiceId) {
        commands.cancel(invoiceId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getFactureByFournisseur/{fournisseur-id}")
    public List<InvoiceResponse> findBySupplier(@PathVariable("fournisseur-id") Long supplierId) {
        return queries.findBySupplier(supplierId);
    }

    @PutMapping("/assignOperateurToFacture/{idOperateur}/{idFacture}")
    public ResponseEntity<Void> assignOperator(
            @PathVariable("idOperateur") Long operatorId,
            @PathVariable("idFacture") Long invoiceId) {
        commands.assignOperator(operatorId, invoiceId);
        return ResponseEntity.ok().build();
    }

}
