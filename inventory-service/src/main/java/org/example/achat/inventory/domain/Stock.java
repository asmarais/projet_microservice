package org.example.achat.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long idStock;

    @NotBlank
    @Column(name = "label", nullable = false)
    private String libelleStock;

    @NotNull
    @PositiveOrZero
    @Column(name = "quantity", nullable = false)
    private Integer qte;

    @NotNull
    @PositiveOrZero
    @Column(name = "minimum_quantity", nullable = false)
    private Integer qteMin;

    public Long getIdStock() {
        return idStock;
    }

    public void setIdStock(Long idStock) {
        this.idStock = idStock;
    }

    public String getLibelleStock() {
        return libelleStock;
    }

    public void setLibelleStock(String libelleStock) {
        this.libelleStock = libelleStock;
    }

    public Integer getQte() {
        return qte;
    }

    public void setQte(Integer qte) {
        this.qte = qte;
    }

    public Integer getQteMin() {
        return qteMin;
    }

    public void setQteMin(Integer qteMin) {
        this.qteMin = qteMin;
    }
}
