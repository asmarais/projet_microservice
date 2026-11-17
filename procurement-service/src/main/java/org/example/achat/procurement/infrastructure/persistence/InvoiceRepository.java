package org.example.achat.procurement.infrastructure.persistence;

import org.example.achat.procurement.domain.Invoice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Override
    @EntityGraph(attributePaths = "detailsFacture")
    List<Invoice> findAll();

    @Override
    @EntityGraph(attributePaths = "detailsFacture")
    Optional<Invoice> findById(Long id);

    @EntityGraph(attributePaths = "detailsFacture")
    List<Invoice> findByFournisseurId(Long fournisseurId);

    List<Invoice> findByDateCreationFactureBetweenAndArchiveeFalse(
            LocalDate startDate, LocalDate endDate);
}
