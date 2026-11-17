package org.example.achat.procurement.application;

import org.example.achat.procurement.api.dto.InvoiceRequest;
import org.example.achat.procurement.api.dto.InvoiceLineRequest;
import org.example.achat.procurement.api.dto.InvoiceResponse;
import org.example.achat.procurement.api.dto.ProductResponse;
import org.example.achat.procurement.domain.Invoice;
import org.example.achat.procurement.domain.InvoiceLine;
import org.example.achat.procurement.domain.InvoiceOperatorAssignment;
import org.example.achat.procurement.infrastructure.client.OperatorClient;
import org.example.achat.procurement.infrastructure.client.ProductClient;
import org.example.achat.procurement.infrastructure.client.SupplierClient;
import org.example.achat.procurement.infrastructure.persistence.InvoiceOperatorAssignmentRepository;
import org.example.achat.procurement.infrastructure.persistence.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class InvoiceCommandService {

    private final InvoiceRepository repository;
    private final InvoiceOperatorAssignmentRepository assignmentRepository;
    private final SupplierClient supplierClient;
    private final OperatorClient operatorClient;
    private final ProductClient productClient;
    private final InvoicePersistenceService persistence;
    private final InvoiceResponseMapper responseMapper;

    public InvoiceCommandService(
            InvoiceRepository repository,
            InvoiceOperatorAssignmentRepository assignmentRepository,
            SupplierClient supplierClient,
            OperatorClient operatorClient,
            ProductClient productClient,
            InvoicePersistenceService persistence,
            InvoiceResponseMapper responseMapper) {
        this.repository = repository;
        this.assignmentRepository = assignmentRepository;
        this.supplierClient = supplierClient;
        this.operatorClient = operatorClient;
        this.productClient = productClient;
        this.persistence = persistence;
        this.responseMapper = responseMapper;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public InvoiceResponse create(InvoiceRequest request) {
        if (request.fournisseurId() != null) {
            try {
                supplierClient.findSupplier(request.fournisseurId());
            } catch (feign.FeignException.NotFound exception) {
                throw new ExternalReferenceNotFoundException("Fournisseur", request.fournisseurId());
            } catch (feign.FeignException exception) {
                throw new DependencyUnavailableException("supplier", exception);
            }
        }
        Invoice invoice = new Invoice();
        invoice.setMontantRemise(request.montantRemise());
        invoice.setMontantFacture(request.montantFacture());
        invoice.setDateCreationFacture(request.dateCreationFacture());
        invoice.setDateDerniereModificationFacture(request.dateDerniereModificationFacture());
        invoice.setArchivee(request.archivee());
        invoice.setFournisseurId(request.fournisseurId());
        List<InvoiceLine> lines = new ArrayList<>();
        float invoiceTotal = 0;
        float invoiceDiscount = 0;
        if (request.detailsFacture() != null) {
            for (InvoiceLineRequest lineRequest : request.detailsFacture()) {
                ProductResponse product = findProduct(lineRequest.produit().idProduit());
                float grossTotal = lineRequest.qteCommandee() * product.prix();
                float discount = (grossTotal * lineRequest.pourcentageRemise()) / 100;
                float netTotal = grossTotal - discount;

                InvoiceLine line = new InvoiceLine();
                line.setQteCommandee(lineRequest.qteCommandee());
                line.setPourcentageRemise(lineRequest.pourcentageRemise());
                line.setMontantRemise(discount);
                line.setPrixTotalDetail(netTotal);
                line.setProductId(product.idProduit());
                line.setProductCode(product.codeProduit());
                line.setProductLabel(product.libelleProduit());
                line.setProductPrice(product.prix());
                line.setProductStockId(product.stockId());
                line.setProductCategoryId(product.categorieProduitId());
                lines.add(line);

                invoiceTotal += netTotal;
                invoiceDiscount += discount;
            }
        }
        if (!lines.isEmpty()) {
            invoice.setMontantFacture(invoiceTotal);
            invoice.setMontantRemise(invoiceDiscount);
        }
        return responseMapper.toResponse(persistence.save(invoice, lines));
    }

    public void cancel(Long id) {
        repository.findById(id).ifPresent(invoice -> {
            invoice.setArchivee(true);
            repository.save(invoice);
        });
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void assignOperator(Long operatorId, Long invoiceId) {
        Invoice invoice = repository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        try {
            operatorClient.findOperator(operatorId);
        } catch (feign.FeignException.NotFound exception) {
            throw new ExternalReferenceNotFoundException("Operateur", operatorId);
        } catch (feign.FeignException exception) {
            throw new DependencyUnavailableException("user", exception);
        }
        InvoiceOperatorAssignment assignment = new InvoiceOperatorAssignment();
        assignment.setInvoice(invoice);
        assignment.setOperatorId(operatorId);
        assignmentRepository.save(assignment);
    }

    private ProductResponse findProduct(Long productId) {
        try {
            ProductResponse product = productClient.findProduct(productId);
            if (product == null
                    || product.idProduit() == null
                    || !productId.equals(product.idProduit())
                    || product.prix() == null) {
                throw new DependencyUnavailableException(
                        "product", new IllegalStateException("Product Service returned an invalid response"));
            }
            return product;
        } catch (feign.FeignException.NotFound exception) {
            throw new ExternalReferenceNotFoundException("Produit", productId);
        } catch (feign.FeignException exception) {
            throw new DependencyUnavailableException("product", exception);
        }
    }
}
