package org.example.achat.product.infrastructure.persistence;

import org.example.achat.product.domain.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @EntityGraph(attributePaths = "categorieProduit")
    List<Product> findAll();

    @Override
    @EntityGraph(attributePaths = "categorieProduit")
    Optional<Product> findById(Long id);
}
