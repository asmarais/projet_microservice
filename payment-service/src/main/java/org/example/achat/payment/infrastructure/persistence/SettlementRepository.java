package org.example.achat.payment.infrastructure.persistence;

import org.example.achat.payment.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByFactureId(Long factureId);

    @Query("""
            select coalesce(sum(s.montantPaye), 0)
            from Settlement s
            where s.dateReglement between :startDate and :endDate
            """)
    float getRevenueBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
