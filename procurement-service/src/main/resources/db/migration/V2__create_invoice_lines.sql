CREATE TABLE invoice_lines (
    id_detail_facture BIGINT NOT NULL AUTO_INCREMENT,
    qte_commandee INT,
    prix_total_detail FLOAT NOT NULL,
    pourcentage_remise INT,
    montant_remise FLOAT NOT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(255),
    product_label VARCHAR(255),
    product_price FLOAT,
    product_stock_id BIGINT,
    product_category_id BIGINT,
    invoice_id BIGINT NOT NULL,
    PRIMARY KEY (id_detail_facture),
    CONSTRAINT fk_invoice_line_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id_facture),
    INDEX ix_invoice_line_invoice (invoice_id),
    INDEX ix_invoice_line_product (product_id)
);
