CREATE TABLE settlements (
    id_reglement BIGINT NOT NULL AUTO_INCREMENT,
    montant_paye FLOAT NOT NULL,
    montant_restant FLOAT NOT NULL,
    payee BOOLEAN NOT NULL,
    date_reglement DATE NOT NULL,
    invoice_id BIGINT NOT NULL,
    PRIMARY KEY (id_reglement),
    INDEX ix_settlement_invoice (invoice_id),
    INDEX ix_settlement_date (date_reglement)
);
