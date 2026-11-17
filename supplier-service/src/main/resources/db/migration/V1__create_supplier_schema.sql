CREATE TABLE supplier_details (
    id_detail_fournisseur BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255),
    date_debut_collaboration DATE,
    adresse VARCHAR(255),
    matricule VARCHAR(255),
    PRIMARY KEY (id_detail_fournisseur)
);

CREATE TABLE activity_sectors (
    id_secteur_activite BIGINT NOT NULL AUTO_INCREMENT,
    code_secteur_activite VARCHAR(255),
    libelle_secteur_activite VARCHAR(255),
    PRIMARY KEY (id_secteur_activite)
);

CREATE TABLE suppliers (
    id_fournisseur BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(255),
    libelle VARCHAR(255),
    categorie_fournisseur VARCHAR(32),
    supplier_detail_id BIGINT,
    PRIMARY KEY (id_fournisseur),
    CONSTRAINT uk_supplier_detail UNIQUE (supplier_detail_id),
    CONSTRAINT fk_supplier_detail FOREIGN KEY (supplier_detail_id)
        REFERENCES supplier_details (id_detail_fournisseur)
);

CREATE TABLE supplier_activity_sectors (
    supplier_id BIGINT NOT NULL,
    activity_sector_id BIGINT NOT NULL,
    PRIMARY KEY (supplier_id, activity_sector_id),
    CONSTRAINT fk_supplier_activity_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers (id_fournisseur),
    CONSTRAINT fk_supplier_activity_sector FOREIGN KEY (activity_sector_id)
        REFERENCES activity_sectors (id_secteur_activite)
);
