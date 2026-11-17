package org.example.achat.procurement.application;

import org.example.achat.procurement.api.dto.InvoiceLineResponse;
import org.example.achat.procurement.api.dto.InvoiceResponse;
import org.example.achat.procurement.api.dto.ProductResponse;
import org.example.achat.procurement.domain.Invoice;
import org.example.achat.procurement.domain.InvoiceLine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvoiceResponseMapper {

    public InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceLineResponse> lines = invoice.getDetailsFacture() == null
                ? List.of()
                : invoice.getDetailsFacture().stream().map(this::toLineResponse).toList();
        return new InvoiceResponse(
                invoice.getIdFacture(),
                invoice.getMontantRemise(),
                invoice.getMontantFacture(),
                invoice.getDateCreationFacture(),
                invoice.getDateDerniereModificationFacture(),
                invoice.getArchivee(),
                lines);
    }

    private InvoiceLineResponse toLineResponse(InvoiceLine line) {
        ProductResponse product = new ProductResponse(
                line.getProductId(),
                line.getProductCode(),
                line.getProductLabel(),
                line.getProductPrice(),
                line.getProductStockId(),
                line.getProductCategoryId());
        return new InvoiceLineResponse(
                line.getIdDetailFacture(),
                line.getQteCommandee(),
                line.getPrixTotalDetail(),
                line.getPourcentageRemise(),
                line.getMontantRemise(),
                product);
    }
}
