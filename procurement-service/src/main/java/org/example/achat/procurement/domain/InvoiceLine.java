package org.example.achat.procurement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoice_lines")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDetailFacture;

    private Integer qteCommandee;

    private float prixTotalDetail;

    private Integer pourcentageRemise;

    private float montantRemise;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "product_label")
    private String productLabel;

    @Column(name = "product_price")
    private Float productPrice;

    @Column(name = "product_stock_id")
    private Long productStockId;

    @Column(name = "product_category_id")
    private Long productCategoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice facture;

    public Long getIdDetailFacture() {
        return idDetailFacture;
    }

    public Integer getQteCommandee() {
        return qteCommandee;
    }

    public void setQteCommandee(Integer qteCommandee) {
        this.qteCommandee = qteCommandee;
    }

    public float getPrixTotalDetail() {
        return prixTotalDetail;
    }

    public void setPrixTotalDetail(float prixTotalDetail) {
        this.prixTotalDetail = prixTotalDetail;
    }

    public Integer getPourcentageRemise() {
        return pourcentageRemise;
    }

    public void setPourcentageRemise(Integer pourcentageRemise) {
        this.pourcentageRemise = pourcentageRemise;
    }

    public float getMontantRemise() {
        return montantRemise;
    }

    public void setMontantRemise(float montantRemise) {
        this.montantRemise = montantRemise;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductLabel() {
        return productLabel;
    }

    public void setProductLabel(String productLabel) {
        this.productLabel = productLabel;
    }

    public Float getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(Float productPrice) {
        this.productPrice = productPrice;
    }

    public Long getProductStockId() {
        return productStockId;
    }

    public void setProductStockId(Long productStockId) {
        this.productStockId = productStockId;
    }

    public Long getProductCategoryId() {
        return productCategoryId;
    }

    public void setProductCategoryId(Long productCategoryId) {
        this.productCategoryId = productCategoryId;
    }

    public Invoice getFacture() {
        return facture;
    }

    public void setFacture(Invoice facture) {
        this.facture = facture;
    }
}
