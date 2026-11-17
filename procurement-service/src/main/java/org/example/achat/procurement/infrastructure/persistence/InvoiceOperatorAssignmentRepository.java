package org.example.achat.procurement.infrastructure.persistence;

import org.example.achat.procurement.domain.InvoiceOperatorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceOperatorAssignmentRepository extends JpaRepository<InvoiceOperatorAssignment, Long> {
}
