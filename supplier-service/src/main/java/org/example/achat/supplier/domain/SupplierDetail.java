package org.example.achat.supplier.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "supplier_details")
public class SupplierDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDetailFournisseur;

    private String email;

    private LocalDate dateDebutCollaboration;

    private String adresse;

    private String matricule;

    public Long getIdDetailFournisseur() {
        return idDetailFournisseur;
    }

    public void setIdDetailFournisseur(Long idDetailFournisseur) {
        this.idDetailFournisseur = idDetailFournisseur;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDateDebutCollaboration() {
        return dateDebutCollaboration;
    }

    public void setDateDebutCollaboration(LocalDate dateDebutCollaboration) {
        this.dateDebutCollaboration = dateDebutCollaboration;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }
}
