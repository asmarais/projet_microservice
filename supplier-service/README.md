# Supplier Service

Standalone Supplier and activity-sector service. It owns its MySQL schema and
does not share persistence classes or database access with the monolith or
other services.

## Runtime

- Java 21
- Spring Boot 4.0.9 and Spring Cloud 2025.1.4
- MySQL database `supplier_db`
- Config Server on port 8888 and Eureka on port 8761
- Default service port 8084 (`SUPPLIER_SERVICE_PORT`)

Provision the `supplier_db` database and an account separately. Set
`SUPPLIER_DB_URL`, `SUPPLIER_DB_USERNAME`, and `SUPPLIER_DB_PASSWORD`; no
credentials are committed. Flyway applies the Supplier-owned schema and
Hibernate validates it. No source data migration is performed.

## API

Implements the legacy `/fournisseurs/**` and `/secteurs-activite/**` CRUD
routes, including sector assignment, with legacy field names and standard
statuses. Procurement's proposed lookup is available at
`/internal/v1/suppliers/{supplierId}` and returns only a supplier reference.
Supplier performance projections are not implemented because no metric
formula or durable event transport has been approved.

Run `.\mvnw.cmd clean verify` for the standalone build.
