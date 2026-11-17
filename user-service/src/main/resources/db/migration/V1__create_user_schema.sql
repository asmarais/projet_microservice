CREATE TABLE operators (
    id_operateur BIGINT NOT NULL AUTO_INCREMENT,
    nom VARCHAR(255),
    prenom VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_operateur)
);
