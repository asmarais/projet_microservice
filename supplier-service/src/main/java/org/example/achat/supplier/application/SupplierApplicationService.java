package org.example.achat.supplier.application;

import org.example.achat.supplier.api.dto.ActivitySectorRequest;
import org.example.achat.supplier.api.dto.ActivitySectorResponse;
import org.example.achat.supplier.api.dto.SupplierDetailRequest;
import org.example.achat.supplier.api.dto.SupplierReferenceResponse;
import org.example.achat.supplier.api.dto.SupplierRequest;
import org.example.achat.supplier.api.dto.SupplierResponse;
import org.example.achat.supplier.domain.ActivitySector;
import org.example.achat.supplier.domain.Supplier;
import org.example.achat.supplier.domain.SupplierDetail;
import org.example.achat.supplier.infrastructure.persistence.ActivitySectorRepository;
import org.example.achat.supplier.infrastructure.persistence.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SupplierApplicationService {

    private final SupplierRepository supplierRepository;
    private final ActivitySectorRepository sectorRepository;

    public SupplierApplicationService(
            SupplierRepository supplierRepository,
            ActivitySectorRepository sectorRepository) {
        this.supplierRepository = supplierRepository;
        this.sectorRepository = sectorRepository;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAllSuppliers() {
        return supplierRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse findSupplier(Long id) {
        return toResponse(findSupplierEntity(id));
    }

    @Transactional(readOnly = true)
    public SupplierReferenceResponse findReference(Long id) {
        Supplier supplier = findSupplierEntity(id);
        return new SupplierReferenceResponse(
                supplier.getIdFournisseur(), supplier.getCode(), supplier.getLibelle());
    }

    public SupplierResponse createSupplier(SupplierRequest request) {
        Supplier supplier = new Supplier();
        apply(supplier, request);
        if (supplier.getDetailFournisseur() == null) {
            SupplierDetail detail = new SupplierDetail();
            detail.setDateDebutCollaboration(LocalDate.now());
            supplier.setDetailFournisseur(detail);
        }
        return toResponse(supplierRepository.save(supplier));
    }

    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = findSupplierEntity(id);
        apply(supplier, request);
        return toResponse(supplierRepository.save(supplier));
    }

    public void deleteSupplier(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new SupplierNotFoundException(id);
        }
        supplierRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ActivitySectorResponse> findAllSectors() {
        return sectorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ActivitySectorResponse findSector(Long id) {
        return toResponse(findSectorEntity(id));
    }

    public ActivitySectorResponse createSector(ActivitySectorRequest request) {
        ActivitySector sector = new ActivitySector();
        sector.setCodeSecteurActivite(request.codeSecteurActivite());
        sector.setLibelleSecteurActivite(request.libelleSecteurActivite());
        return toResponse(sectorRepository.save(sector));
    }

    public ActivitySectorResponse updateSector(Long id, ActivitySectorRequest request) {
        ActivitySector sector = findSectorEntity(id);
        sector.setCodeSecteurActivite(request.codeSecteurActivite());
        sector.setLibelleSecteurActivite(request.libelleSecteurActivite());
        return toResponse(sectorRepository.save(sector));
    }

    public void deleteSector(Long id) {
        if (!sectorRepository.existsById(id)) {
            throw new ActivitySectorNotFoundException(id);
        }
        sectorRepository.deleteById(id);
    }

    public SupplierResponse assignSector(Long supplierId, Long sectorId) {
        Supplier supplier = findSupplierEntity(supplierId);
        ActivitySector sector = findSectorEntity(sectorId);
        supplier.getSecteurActivites().add(sector);
        return toResponse(supplierRepository.save(supplier));
    }

    private Supplier findSupplierEntity(Long id) {
        return supplierRepository.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
    }

    private ActivitySector findSectorEntity(Long id) {
        return sectorRepository.findById(id).orElseThrow(() -> new ActivitySectorNotFoundException(id));
    }

    private void apply(Supplier supplier, SupplierRequest request) {
        supplier.setCode(request.code());
        supplier.setLibelle(request.libelle());
        supplier.setCategorieFournisseur(request.categorieFournisseur());
        if (request.detailFournisseur() != null) {
            supplier.setDetailFournisseur(toDetail(request.detailFournisseur()));
        }
    }

    private SupplierDetail toDetail(SupplierDetailRequest request) {
        SupplierDetail detail = new SupplierDetail();
        detail.setEmail(request.email());
        detail.setDateDebutCollaboration(request.dateDebutCollaboration());
        detail.setAdresse(request.adresse());
        detail.setMatricule(request.matricule());
        return detail;
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getIdFournisseur(),
                supplier.getCode(),
                supplier.getLibelle(),
                supplier.getCategorieFournisseur(),
                supplier.getDetailFournisseur() == null
                        ? null : supplier.getDetailFournisseur().getIdDetailFournisseur(),
                supplier.getSecteurActivites().stream()
                        .map(ActivitySector::getIdSecteurActivite)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }

    private ActivitySectorResponse toResponse(ActivitySector sector) {
        return new ActivitySectorResponse(
                sector.getIdSecteurActivite(),
                sector.getCodeSecteurActivite(),
                sector.getLibelleSecteurActivite());
    }
}
