package org.example.achat.inventory.infrastructure.persistence;

import org.example.achat.inventory.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Long> {

    @Query("SELECT s FROM Stock s WHERE s.qte < s.qteMin")
    List<Stock> findStocksBelowMinimumQuantity();
}
