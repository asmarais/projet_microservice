package org.example.achat.payment.application;

import org.example.achat.payment.api.dto.InvoiceEligibilityResponse;
import org.example.achat.payment.api.dto.InvoiceTotalResponse;
import org.example.achat.payment.api.dto.SettlementRequest;
import org.example.achat.payment.api.dto.SettlementResponse;
import org.example.achat.payment.domain.Settlement;
import org.example.achat.payment.infrastructure.client.InvoiceClient;
import org.example.achat.payment.infrastructure.persistence.SettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SettlementApplicationService {

    private final SettlementRepository repository;
    private final InvoiceClient invoiceClient;

    public SettlementApplicationService(SettlementRepository repository, InvoiceClient invoiceClient) {
        this.repository = repository;
        this.invoiceClient = invoiceClient;
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SettlementResponse findById(Long id) {
        return toResponse(repository.findById(id).orElseThrow(() -> new SettlementNotFoundException(id)));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SettlementResponse create(SettlementRequest request) {
        validateInvoiceEligibility(request.factureId());
        Settlement settlement = new Settlement();
        apply(settlement, request);
        return toResponse(repository.save(settlement));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SettlementResponse update(Long id, SettlementRequest request) {
        Settlement settlement = repository.findById(id).orElseThrow(() -> new SettlementNotFoundException(id));
        validateInvoiceEligibility(request.factureId());
        apply(settlement, request);
        return toResponse(repository.save(settlement));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new SettlementNotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> findByInvoice(Long invoiceId) {
        return repository.findByFactureId(invoiceId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public float getRevenue(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        return repository.getRevenueBetweenDates(startDate, endDate);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public float getRecoveryRate(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        InvoiceTotalResponse invoiceTotal = getInvoiceTotal(startDate, endDate);
        float paymentTotal = repository.getRevenueBetweenDates(startDate, endDate);
        return (paymentTotal / invoiceTotal.total()) * 100;
    }

    private void validateInvoiceEligibility(Long invoiceId) {
        final InvoiceEligibilityResponse eligibility;
        try {
            eligibility = invoiceClient.getPaymentEligibility(invoiceId);
        } catch (feign.FeignException.NotFound exception) {
            throw new InvoiceNotFoundException(invoiceId);
        } catch (feign.FeignException exception) {
            throw new InvoiceServiceUnavailableException(exception);
        }
        if (eligibility == null
                || !invoiceId.equals(eligibility.invoiceId())
                || (!eligibility.eligible() && eligibility.reasonCode() == null)) {
            throw new InvoiceServiceUnavailableException(
                    new IllegalStateException("Procurement service returned an invalid eligibility response"));
        }
        if (!eligibility.eligible()) {
            throw new InvoiceNotEligibleException(eligibility.reasonCode());
        }
    }

    private InvoiceTotalResponse getInvoiceTotal(LocalDate startDate, LocalDate endDate) {
        try {
            InvoiceTotalResponse total = invoiceClient.getInvoiceTotal(startDate, endDate);
            if (total == null) {
                throw new InvoiceServiceUnavailableException(
                        new IllegalStateException("Procurement service returned an empty invoice total"));
            }
            return total;
        } catch (feign.FeignException exception) {
            throw new InvoiceServiceUnavailableException(exception);
        }
    }

    private void apply(Settlement settlement, SettlementRequest request) {
        settlement.setMontantPaye(request.montantPaye());
        settlement.setMontantRestant(request.montantRestant());
        settlement.setPayee(request.payee());
        settlement.setDateReglement(request.dateReglement());
        settlement.setFactureId(request.factureId());
    }

    private SettlementResponse toResponse(Settlement settlement) {
        return new SettlementResponse(
                settlement.getIdReglement(),
                settlement.getMontantPaye(),
                settlement.getMontantRestant(),
                settlement.getPayee(),
                settlement.getDateReglement(),
                settlement.getFactureId());
    }
}
