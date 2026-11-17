package org.example.achat.supplier.infrastructure.persistence;

import org.example.achat.supplier.domain.Supplier;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    @Override
    @EntityGraph(attributePaths = {"detailFournisseur", "secteurActivites"})
    List<Supplier> findAll();

    @Override
    @EntityGraph(attributePaths = {"detailFournisseur", "secteurActivites"})
    Optional<Supplier> findById(Long id);
}
