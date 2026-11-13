CREATE TEMPORARY TABLE migration_stage_categories (
    id_category BIGINT NOT NULL,
    category_code VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL
);

CREATE TEMPORARY TABLE migration_stage_products (
    id_product BIGINT NOT NULL,
    code_product VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL,
    price FLOAT NOT NULL,
    creation_date DATE NULL,
    last_modified_date DATE NULL,
    category_id BIGINT NULL
);

LOAD DATA LOCAL INFILE 'product-categories.csv'
INTO TABLE migration_stage_categories
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(id_category, category_code, label);

LOAD DATA LOCAL INFILE 'products.csv'
INTO TABLE migration_stage_products
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(id_product, code_product, label, price, @creation_date, @last_modified_date, @category_id)
SET creation_date = NULLIF(@creation_date, ''),
    last_modified_date = NULLIF(@last_modified_date, ''),
    category_id = NULLIF(@category_id, '');

START TRANSACTION;

INSERT INTO product_categories (id_category, category_code, label)
SELECT id_category, category_code, label
FROM migration_stage_categories;

INSERT INTO products (
    id_product,
    code_product,
    label,
    price,
    creation_date,
    last_modified_date,
    category_id
)
SELECT
    id_product,
    code_product,
    label,
    price,
    creation_date,
    last_modified_date,
    category_id
FROM migration_stage_products;

COMMIT;

SELECT
    (SELECT COUNT(*) FROM migration_stage_categories) AS exported_categories,
    (SELECT COUNT(*) FROM product_categories) AS imported_categories,
    (SELECT COUNT(*) FROM migration_stage_products) AS exported_products,
    (SELECT COUNT(*) FROM products) AS imported_products;

SELECT source.id_category AS missing_category_id
FROM migration_stage_categories source
LEFT JOIN product_categories target ON target.id_category = source.id_category
WHERE target.id_category IS NULL;

SELECT source.id_product AS missing_product_id
FROM migration_stage_products source
LEFT JOIN products target ON target.id_product = source.id_product
WHERE target.id_product IS NULL;

SELECT source.id_product AS mismatched_product_id
FROM migration_stage_products source
JOIN products target ON target.id_product = source.id_product
WHERE NOT (target.code_product <=> source.code_product)
   OR NOT (target.label <=> source.label)
   OR NOT (target.price <=> source.price)
   OR NOT (target.creation_date <=> source.creation_date)
   OR NOT (target.last_modified_date <=> source.last_modified_date)
   OR NOT (target.category_id <=> source.category_id);
