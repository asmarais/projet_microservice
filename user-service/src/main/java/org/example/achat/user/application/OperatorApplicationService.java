package org.example.achat.user.application;

import org.example.achat.user.api.dto.OperatorReferenceResponse;
import org.example.achat.user.api.dto.OperatorRequest;
import org.example.achat.user.api.dto.OperatorResponse;
import org.example.achat.user.domain.Operator;
import org.example.achat.user.infrastructure.persistence.OperatorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class OperatorApplicationService {

    private final OperatorRepository repository;
    private final PasswordEncoder passwordEncoder;

    public OperatorApplicationService(OperatorRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<OperatorResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OperatorResponse findById(Long id) {
        return toResponse(findOperator(id));
    }

    @Transactional(readOnly = true)
    public OperatorReferenceResponse findReference(Long id) {
        Operator operator = findOperator(id);
        return new OperatorReferenceResponse(operator.getIdOperateur(), operator.getNom(), operator.getPrenom());
    }

    public OperatorResponse create(OperatorRequest request) {
        Operator operator = new Operator();
        operator.setNom(request.nom());
        operator.setPrenom(request.prenom());
        operator.setPasswordHash(passwordEncoder.encode(request.password()));
        return toResponse(repository.save(operator));
    }

    public OperatorResponse update(Long id, OperatorRequest request) {
        Operator operator = findOperator(id);
        operator.setNom(request.nom());
        operator.setPrenom(request.prenom());
        operator.setPasswordHash(passwordEncoder.encode(request.password()));
        return toResponse(repository.save(operator));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new OperatorNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private Operator findOperator(Long id) {
        return repository.findById(id).orElseThrow(() -> new OperatorNotFoundException(id));
    }

    private OperatorResponse toResponse(Operator operator) {
        // Procurement owns invoice/operator assignments; this legacy projection is intentionally empty.
        return new OperatorResponse(operator.getIdOperateur(), operator.getNom(), operator.getPrenom(), Set.of());
    }
}
