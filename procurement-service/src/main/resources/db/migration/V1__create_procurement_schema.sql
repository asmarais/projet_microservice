CREATE TABLE invoices (
    id_facture BIGINT NOT NULL AUTO_INCREMENT,
    montant_remise FLOAT NOT NULL,
    montant_facture FLOAT NOT NULL,
    date_creation_facture DATE,
    date_derniere_modification_facture DATE,
    archivee BOOLEAN,
    supplier_id BIGINT,
    PRIMARY KEY (id_facture),
    INDEX ix_invoice_supplier (supplier_id),
    INDEX ix_invoice_creation_archive (date_creation_facture, archivee)
);

CREATE TABLE invoice_operator_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_operator_assignment_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id_facture),
    INDEX ix_invoice_operator_assignment_operator (operator_id)
);
