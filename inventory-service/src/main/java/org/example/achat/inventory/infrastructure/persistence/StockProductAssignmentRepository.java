package org.example.achat.inventory.infrastructure.persistence;

import org.example.achat.inventory.domain.StockProductAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockProductAssignmentRepository extends JpaRepository<StockProductAssignment, Long> {

    boolean existsByStock_IdStock(Long stockId);

    StockProductAssignment findFirstByProductId(Long productId);
}
