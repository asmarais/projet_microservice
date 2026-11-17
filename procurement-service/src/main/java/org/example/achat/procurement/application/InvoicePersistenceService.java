package org.example.achat.procurement.application;

import org.example.achat.procurement.domain.Invoice;
import org.example.achat.procurement.domain.InvoiceLine;
import org.example.achat.procurement.infrastructure.persistence.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InvoicePersistenceService {

    private final InvoiceRepository repository;

    public InvoicePersistenceService(InvoiceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Invoice save(Invoice invoice, List<InvoiceLine> lines) {
        lines.forEach(line -> line.setFacture(invoice));
        invoice.setDetailsFacture(lines);
        return repository.save(invoice);
    }
}
