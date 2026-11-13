CREATE TABLE product_categories (
    id_category BIGINT NOT NULL AUTO_INCREMENT,
    category_code VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_category)
) ENGINE=InnoDB;

CREATE TABLE products (
    id_product BIGINT NOT NULL AUTO_INCREMENT,
    code_product VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL,
    price FLOAT NOT NULL,
    creation_date DATE NULL,
    last_modified_date DATE NULL,
    category_id BIGINT NULL,
    PRIMARY KEY (id_product),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES product_categories (id_category)
) ENGINE=InnoDB;
