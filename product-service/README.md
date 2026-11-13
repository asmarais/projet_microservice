# Product Service

Standalone Product and Product Category service. It is intentionally not a
module of `infrastructure/pom.xml`.

## Runtime requirements

- Java 21
- MySQL with an empty, provisioned `product_service` database
- Config Server on port 8888, with the committed `product-service.yml`
  available in its Git-backed repository
- Eureka Discovery Server on port 8761

The service defaults to port 8082 and registers as `product-service`. Flyway
creates and versions only Product-owned tables. Hibernate is set to validate
the Flyway schema rather than create or update it. Configure
`PRODUCT_DB_URL`, `PRODUCT_DB_USERNAME`, and `PRODUCT_DB_PASSWORD` outside
source control; no credentials are stored in the Config repository.

## Build and run (PowerShell)

```powershell
.\mvnw.cmd clean verify

$env:PRODUCT_DB_URL = "jdbc:mysql://localhost:3306/product_service?serverTimezone=UTC"
$env:PRODUCT_DB_USERNAME = "product_service"
$env:PRODUCT_DB_PASSWORD = "<set locally; do not commit>"
.\mvnw.cmd spring-boot:run
```

Start Config Server and Eureka before launching the service. Provision the
database and account separately; Flyway does not create databases or users.
Health is available at `/actuator/health`, and API documentation at
`/swagger-ui.html` and `/v3/api-docs`.

## API scope

The service provides the legacy Product CRUD routes under `/produits` and
Category CRUD routes under `/categories-produits`. Search is deferred because
the legacy API has no search contract. Product price remains `float` pending
Finance approval. Product codes are not unique-constrained.

Product responses retain the legacy `stockId` JSON field as `null`; the
service owns no stock data. The stock assignment endpoint is not implemented.
Do not route any Product prefix through the Gateway yet. Per the approved
scope, all Gateway cutover waits until Inventory defines the stock assignment
and `stockId` compatibility contract. The existing Gateway fallback remains
unchanged.

Category references are Product-owned and non-cascading. Database constraint
conflicts return HTTP 409. Product deletion cannot detect references in
Inventory or Procurement; define that cross-service policy before any
authoritative data cutover.

## Data migration

No source schema was supplied or inspected, and no data has been transferred.
See [`scripts/migration/README.md`](scripts/migration/README.md) for the
canonical export/import template and required preflight/reconciliation steps.
