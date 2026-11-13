# Product data import preparation

The Product database is created by Flyway. No source data has been inspected
or copied. Do not execute the import until a DBA has verified the deployed
source DDL, selected the correct source database, reviewed source duplicates
and invalid rows, and produced the canonical CSV exports below.

The import deliberately consumes a reviewed export instead of naming guessed
monolith tables. The DBA must map the verified source columns and category
relationship into these UTF-8, comma-separated, LF-terminated files:

- `product-categories.csv`: `id_category,category_code,label`
- `products.csv`: `id_product,code_product,label,price,creation_date,last_modified_date,category_id`

IDs must be the approved preserved Product and Category IDs. Dates use
`YYYY-MM-DD` or an empty field. Empty category IDs and dates are imported as
SQL `NULL`. Prices use the approved legacy-float-compatible representation;
confirm the deployed source numeric type and check round-trip values before
export because its schema has not been verified. This service initially
stores `price` as MySQL `FLOAT`; it does not convert values to `DECIMAL`.

## Preconditions

1. Provision an empty Product Service database and run the service once so
   Flyway applies `V1__create_product_catalog.sql`.
2. Verify the source DDL and the CSV mapping. Do not infer physical source
   table or column names from entity annotations.
3. Confirm both target tables are empty and both CSV files are complete.
4. Back up the target database. Keep the monolith unchanged and authoritative.
5. Execute with a migration-only account and `LOCAL INFILE` enabled for the
   client. Keep the CSV files in the MySQL client's working directory.

## Import and verification

Run `import-canonical-csv.sql` once in a single MySQL client session against
the Product Service database, with `--local-infile=1` enabled. Do not use the
client's force/continue-on-error option. The script stages the export in
temporary tables, inserts Categories before Products, preserves IDs, and
prints row counts, missing IDs, and mismatched Product values after import.
The foreign key rejects Products with missing Categories; the script does not
disable constraints, truncate tables, drop data, or modify monolith tables.

Stop and investigate if the client returns an error, any count differs, or
any verification query returns rows. Because the deployed source schema and
price representation are not known, this template has not been executed or
validated against real source data. Review rollback/retry against a fresh
empty target database; do not rerun it against a populated target.
