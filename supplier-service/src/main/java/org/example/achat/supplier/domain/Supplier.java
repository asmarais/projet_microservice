package org.example.achat.supplier.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFournisseur;

    private String code;

    private String libelle;

    @Enumerated(EnumType.STRING)
    private SupplierCategory categorieFournisseur;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_detail_id", unique = true)
    private SupplierDetail detailFournisseur;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "supplier_activity_sectors",
            joinColumns = @JoinColumn(name = "supplier_id"),
            inverseJoinColumns = @JoinColumn(name = "activity_sector_id"))
    private Set<ActivitySector> secteurActivites = new LinkedHashSet<>();

    public Long getIdFournisseur() {
        return idFournisseur;
    }

    public void setIdFournisseur(Long idFournisseur) {
        this.idFournisseur = idFournisseur;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public SupplierCategory getCategorieFournisseur() {
        return categorieFournisseur;
    }

    public void setCategorieFournisseur(SupplierCategory categorieFournisseur) {
        this.categorieFournisseur = categorieFournisseur;
    }

    public SupplierDetail getDetailFournisseur() {
        return detailFournisseur;
    }

    public void setDetailFournisseur(SupplierDetail detailFournisseur) {
        this.detailFournisseur = detailFournisseur;
    }

    public Set<ActivitySector> getSecteurActivites() {
        return secteurActivites;
    }

    public void setSecteurActivites(Set<ActivitySector> secteurActivites) {
        this.secteurActivites = secteurActivites == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(secteurActivites);
    }
}
