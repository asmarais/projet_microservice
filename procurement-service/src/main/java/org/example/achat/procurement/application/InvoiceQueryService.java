package org.example.achat.procurement.application;

import org.example.achat.procurement.api.dto.InvoicePaymentEligibilityResponse;
import org.example.achat.procurement.api.dto.InvoiceResponse;
import org.example.achat.procurement.api.dto.InvoiceTotalResponse;
import org.example.achat.procurement.domain.Invoice;
import org.example.achat.procurement.infrastructure.client.SupplierClient;
import org.example.achat.procurement.infrastructure.persistence.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InvoiceQueryService {

    private final InvoiceRepository repository;
    private final SupplierClient supplierClient;
    private final InvoiceResponseMapper responseMapper;

    public InvoiceQueryService(
            InvoiceRepository repository,
            SupplierClient supplierClient,
            InvoiceResponseMapper responseMapper) {
        this.repository = repository;
        this.supplierClient = supplierClient;
        this.responseMapper = responseMapper;
    }

    public List<InvoiceResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public InvoiceResponse findById(Long id) {
        return repository.findById(id).map(this::toResponse).orElse(null);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<InvoiceResponse> findBySupplier(Long supplierId) {
        try {
            supplierClient.findSupplier(supplierId);
        } catch (feign.FeignException.NotFound exception) {
            throw new ExternalReferenceNotFoundException("Fournisseur", supplierId);
        } catch (feign.FeignException exception) {
            throw new DependencyUnavailableException("supplier", exception);
        }
        return repository.findByFournisseurId(supplierId).stream().map(this::toResponse).toList();
    }

    public InvoicePaymentEligibilityResponse paymentEligibility(Long invoiceId) {
        Invoice invoice = repository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        boolean eligible = !Boolean.TRUE.equals(invoice.getArchivee());
        return new InvoicePaymentEligibilityResponse(
                invoiceId,
                eligible,
                eligible ? null : "INVOICE_ARCHIVED");
    }

    public InvoiceTotalResponse totalForPaymentReporting(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        float invoiceTotal = (float) repository
                .findByDateCreationFactureBetweenAndArchiveeFalse(startDate, endDate)
                .stream()
                .mapToDouble(Invoice::getMontantFacture)
                .sum();
        return new InvoiceTotalResponse(invoiceTotal);
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return responseMapper.toResponse(invoice);
    }
}
