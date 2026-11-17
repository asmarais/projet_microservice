package org.example.achat.supplier.infrastructure.persistence;

import org.example.achat.supplier.domain.ActivitySector;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivitySectorRepository extends JpaRepository<ActivitySector, Long> {
}
