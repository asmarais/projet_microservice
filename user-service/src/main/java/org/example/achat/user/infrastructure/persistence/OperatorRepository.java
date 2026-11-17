package org.example.achat.user.infrastructure.persistence;

import org.example.achat.user.domain.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, Long> {
}
