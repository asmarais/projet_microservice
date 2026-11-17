CREATE TABLE stocks (
    stock_id BIGINT NOT NULL AUTO_INCREMENT,
    label VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    minimum_quantity INT NOT NULL,
    PRIMARY KEY (stock_id),
    CONSTRAINT chk_stocks_quantity_nonnegative CHECK (quantity >= 0),
    CONSTRAINT chk_stocks_minimum_quantity_nonnegative CHECK (minimum_quantity >= 0)
) ENGINE=InnoDB;

CREATE TABLE stock_product_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_stock_product_assignments_stock
        FOREIGN KEY (stock_id) REFERENCES stocks (stock_id),
    INDEX idx_stock_product_assignments_product_id (product_id)
) ENGINE=InnoDB;
