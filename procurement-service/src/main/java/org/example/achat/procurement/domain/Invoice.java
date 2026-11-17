package org.example.achat.procurement.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFacture;

    private float montantRemise;

    private float montantFacture;

    private LocalDate dateCreationFacture;

    private LocalDate dateDerniereModificationFacture;

    private Boolean archivee;

    @Column(name = "supplier_id")
    private Long fournisseurId;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLine> detailsFacture = new ArrayList<>();

    public Long getIdFacture() {
        return idFacture;
    }

    public void setIdFacture(Long idFacture) {
        this.idFacture = idFacture;
    }

    public float getMontantRemise() {
        return montantRemise;
    }

    public void setMontantRemise(float montantRemise) {
        this.montantRemise = montantRemise;
    }

    public float getMontantFacture() {
        return montantFacture;
    }

    public void setMontantFacture(float montantFacture) {
        this.montantFacture = montantFacture;
    }

    public LocalDate getDateCreationFacture() {
        return dateCreationFacture;
    }

    public void setDateCreationFacture(LocalDate dateCreationFacture) {
        this.dateCreationFacture = dateCreationFacture;
    }

    public LocalDate getDateDerniereModificationFacture() {
        return dateDerniereModificationFacture;
    }

    public void setDateDerniereModificationFacture(LocalDate dateDerniereModificationFacture) {
        this.dateDerniereModificationFacture = dateDerniereModificationFacture;
    }

    public Boolean getArchivee() {
        return archivee;
    }

    public void setArchivee(Boolean archivee) {
        this.archivee = archivee;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public void setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
    }

    public List<InvoiceLine> getDetailsFacture() {
        return detailsFacture;
    }

    public void setDetailsFacture(List<InvoiceLine> detailsFacture) {
        this.detailsFacture = detailsFacture;
    }
}
