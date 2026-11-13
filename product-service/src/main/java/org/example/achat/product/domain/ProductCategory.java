package org.example.achat.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_categories")
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_category")
    private Long idCategorieProduit;

    @Column(name = "category_code", nullable = false)
    private String codeCategorie;

    @Column(name = "label", nullable = false)
    private String libelleCategorie;

    public Long getIdCategorieProduit() {
        return idCategorieProduit;
    }

    public void setIdCategorieProduit(Long idCategorieProduit) {
        this.idCategorieProduit = idCategorieProduit;
    }

    public String getCodeCategorie() {
        return codeCategorie;
    }

    public void setCodeCategorie(String codeCategorie) {
        this.codeCategorie = codeCategorie;
    }

    public String getLibelleCategorie() {
        return libelleCategorie;
    }

    public void setLibelleCategorie(String libelleCategorie) {
        this.libelleCategorie = libelleCategorie;
    }
}
